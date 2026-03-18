package fi.seamk.kodera.service;

import fi.seamk.kodera.client.GeminiClient;
import fi.seamk.kodera.model.Assignment;
import fi.seamk.kodera.model.Grade;
import fi.seamk.kodera.model.Submission;
import fi.seamk.kodera.repository.AssignmentRepository;
import fi.seamk.kodera.repository.GradeRepository;
import fi.seamk.kodera.repository.SubmissionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.io.ByteArrayInputStream;

@Service
public class GradeService {

    // Keep Gemini prompt bounded so requests do not fail with 400 due to oversized input.
    private static final int MAX_CODE_CONTENT_CHARS = 15000;
    
    @Autowired
    private GeminiClient geminiClient;
    
    @Autowired
    private SubmissionRepository submissionRepository;
    
    @Autowired
    private AssignmentRepository assignmentRepository;
    
    @Autowired
    private GradeRepository gradeRepository;
    
    @Transactional
    public Grade gradeSubmission(Long submissionId) throws Exception {
        Optional<Submission> submissionOpt = submissionRepository.findById(submissionId);
        if (!submissionOpt.isPresent()) {
            throw new RuntimeException("Submission not found");
        }

        Optional<Grade> existingGradeOpt = gradeRepository.findBySubmissionId(submissionId);
        
        Submission submission = submissionOpt.get();
        Optional<Assignment> assignmentOpt = assignmentRepository.findById(submission.getAssignmentId());
        if (!assignmentOpt.isPresent()) {
            throw new RuntimeException("Assignment not found");
        }
        
        Assignment assignment = assignmentOpt.get();
        String codeContent = extractCodeFromZip(submission.getCodeZip());
        
        String prompt = String.format("""
            Olet opettaja joka arvioi opiskelijan koodaustehtävän. Kirjoita palaute SUOMEKSI.
            
            SÄÄNNÖT:
            - EI markdown-muotoilua: ei **tekstiä**, ei *tekstiä*, ei # otsikoita.
            - Arvioi AINOASTAAN opettajan asettamia vaatimuksia.
            - Jos vaatimus täyttyy, kerro lyhyesti että se täyttyy.
            - Jos vaatimus ei täyty, kerro lyhyesti miksi.
            - Älä selitä termejä tai jaarittele.
            - Älä anna arvosanaa lainkaan.
            
            OPETTAJAN VAATIMUKSET:
            %s
            
            OPISKELIJAN KOODI:
            %s
            
            VASTAUSMUOTO:
            Vaatimusten täyttyminen: KYLLÄ tai EI
            Palaute: 2-5 selkeää lausetta opiskelijalle.
            Tarkistusdata:
            REQ|OK|<vaatimuslyhyt>
            REQ|FAIL|<vaatimuslyhyt>
            
            Lisää yksi REQ-rivi jokaista vaatimusta kohti.
            """, assignment.getDescription(), codeContent);
        
        String rawAnalysis = geminiClient.generateAnalysis(prompt);
        int points = extractDeterministicPoints(rawAnalysis);
        String analysis = normalizeFeedback(rawAnalysis, points);
        
        Grade grade = existingGradeOpt.orElseGet(Grade::new);
        grade.setSubmissionId(submissionId);
        grade.setGeminiAnalysis(analysis);
        grade.setPoints(points);
        grade.setFeedback(analysis);
        grade.setGradedAt(LocalDateTime.now());
        
        return gradeRepository.save(grade);
    }
    
    public Optional<Grade> getGrade(Long submissionId) {
        return gradeRepository.findBySubmissionId(submissionId);
    }
    
    private String extractCodeFromZip(byte[] zipBytes) throws Exception {
        StringBuilder code = new StringBuilder();
        ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(zipBytes));
        ZipEntry entry;
        
        while ((entry = zis.getNextEntry()) != null) {
            if (code.length() >= MAX_CODE_CONTENT_CHARS) {
                code.append("\n\n--- [TRUNCATED: liikaa sisältöä analyysiin] ---");
                break;
            }

            if (!entry.isDirectory() && isCodeFile(entry.getName())) {
                code.append("--- ").append(entry.getName()).append(" ---\n");
                byte[] buffer = new byte[1024];
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    if (code.length() >= MAX_CODE_CONTENT_CHARS) {
                        code.append("\n--- [TRUNCATED: tiedoston sisältö katkaistu] ---");
                        break;
                    }

                    String chunk = new String(buffer, 0, len, StandardCharsets.UTF_8);
                    int remaining = MAX_CODE_CONTENT_CHARS - code.length();
                    if (chunk.length() > remaining) {
                        code.append(chunk, 0, Math.max(0, remaining));
                        code.append("\n--- [TRUNCATED: tiedoston sisältö katkaistu] ---");
                        break;
                    }
                    code.append(chunk);
                }
                code.append("\n\n");
            }
        }
        zis.close();
        return code.toString();
    }
    
    private boolean isCodeFile(String filename) {
        return filename.endsWith(".java") || filename.endsWith(".py") || filename.endsWith(".js") 
            || filename.endsWith(".json") || filename.endsWith(".txt") || filename.endsWith(".md");
    }
    
    private int extractPointsFromAnalysis(String analysis) {
        if (analysis == null || analysis.isEmpty()) return 0;

        // Etsi "Arvosana: X/100" tai "Arvosana: X" -muoto
        java.util.regex.Matcher m1 = java.util.regex.Pattern
            .compile("(?:arvosana|pisteet)[:\\s]+(\\d{1,3})\\s*(?:/\\s*100)?",
                java.util.regex.Pattern.CASE_INSENSITIVE)
            .matcher(analysis);
        if (m1.find()) {
            try { return Math.min(100, Math.max(0, Integer.parseInt(m1.group(1)))); }
            catch (NumberFormatException ignored) {}
        }

        // Fallback: etsi X/100 -muoto
        java.util.regex.Matcher m2 = java.util.regex.Pattern
            .compile("(\\d{1,3})\\s*/\\s*100")
            .matcher(analysis);
        if (m2.find()) {
            try { return Math.min(100, Math.max(0, Integer.parseInt(m2.group(1)))); }
            catch (NumberFormatException ignored) {}
        }

        if (analysis.contains("KYLLÄ") || analysis.contains("HYVÄKSYTTY")) return 75;
        return 0;
    }

    private int extractDeterministicPoints(String analysis) {
        if (analysis == null || analysis.isEmpty()) {
            return 0;
        }

        int total = 0;
        int ok = 0;

        for (String line : analysis.split("\\R")) {
            String normalized = line == null ? "" : line.trim();
            if (!normalized.startsWith("REQ|")) {
                continue;
            }

            String[] parts = normalized.split("\\|", 4);
            if (parts.length < 3) {
                continue;
            }

            String status = parts[1].trim().toUpperCase();
            if ("OK".equals(status) || "FAIL".equals(status)) {
                total++;
            }
            if ("OK".equals(status)) {
                ok++;
            }
        }

        if (total == 0) {
            return extractPointsFromAnalysis(analysis);
        }

        return Math.round((ok * 100.0f) / total);
    }

    private String normalizeFeedback(String analysis, int points) {
        if (analysis == null) {
            return "Arvosana: " + points + "/100";
        }

        StringBuilder cleaned = new StringBuilder();
        for (String line : analysis.split("\\R")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("REQ|")) {
                continue;
            }
            if (trimmed.equalsIgnoreCase("Tarkistusdata:")) {
                continue;
            }
            if (trimmed.toLowerCase().startsWith("arvosana:")) {
                continue;
            }
            cleaned.append(line).append("\n");
        }

        String normalized = cleaned.toString().trim();
        if (!normalized.isEmpty()) {
            normalized += "\n\n";
        }
        normalized += "Arvosana: " + points + "/100";
        return normalized;
    }
}
