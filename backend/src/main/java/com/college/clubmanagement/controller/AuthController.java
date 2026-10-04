package com.college.clubmanagement.controller;
import com.college.clubmanagement.entity.ClubMembership;
import com.college.clubmanagement.entity.Student;
import com.college.clubmanagement.repository.ClubMembershipRepository;
import com.college.clubmanagement.repository.StudentRepository;
import com.college.clubmanagement.service.LoggingService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.stream.Collectors;

@Controller
public class AuthController {
    private final StudentRepository studentRepository;
    private final ClubMembershipRepository membershipRepository;
    private final LoggingService loggingService;
    
    public AuthController(StudentRepository studentRepository, ClubMembershipRepository membershipRepository, LoggingService loggingService) {
        this.studentRepository = studentRepository;
        this.membershipRepository = membershipRepository;
        this.loggingService = loggingService;
    }

    @GetMapping("/login") public String loginPage() { return "login"; }
    @GetMapping("/register") public String registerPage() { return "register"; }
    @GetMapping("/logout") public String logout(HttpSession session) { session.invalidate(); return "redirect:/login"; }

    @PostMapping("/login")
    public String loginSubmit(@RequestParam String rollNumber, @RequestParam String password, HttpSession session) {
        if ("superadmin".equals(rollNumber) && "superadmin".equals(password)) {
            session.setAttribute("USER_ROLL", "0"); session.setAttribute("USER_NAME", "Super Admin"); session.setAttribute("IS_SUPER_ADMIN", true);
            return "redirect:/";
        }
        Student student = studentRepository.findById(rollNumber).orElse(null);
        if (student != null && student.getPassword() != null && student.getPassword().equals(password)) {
            session.setAttribute("USER_ROLL", student.getRollNumber()); session.setAttribute("USER_NAME", student.getName());
            return "redirect:/";
        }
        return "redirect:/login?error=InvalidCredentials";
    }

    @GetMapping("/profile")
    public String viewProfile(HttpSession session, Model model) {
        String rollNumber = (String) session.getAttribute("USER_ROLL");
        if (rollNumber == null) return "redirect:/login";
        model.addAttribute("student", studentRepository.findById(rollNumber).orElseThrow());
        model.addAttribute("memberships", membershipRepository.findAll().stream().filter(m -> m.getStudent().getRollNumber().equals(rollNumber)).collect(Collectors.toList()));
        return "profile";
    }

    @PostMapping("/user/resign")
    public String resign(@RequestParam Integer membershipId, HttpSession session) {
        String rollNumber = (String) session.getAttribute("USER_ROLL");
        ClubMembership cm = membershipRepository.findById(membershipId).orElseThrow();
        if(!cm.getStudent().getRollNumber().equals(rollNumber)) return "redirect:/profile?error=Unauthorized";
        if(cm.getRole() != null) {
            cm.setRole(null); membershipRepository.save(cm);
            loggingService.log(rollNumber, "Resign POR", "Resigned from POR");
            return "redirect:/profile?success=Resigned+from+POR";
        } else {
            membershipRepository.delete(cm);
            loggingService.log(rollNumber, "Leave Club", "Left Club " + cm.getClub().getName());
            return "redirect:/profile?success=Left+Club";
        }
    }

    @HeadMapping("/health")
    public ResponseEntity<Void> health() {
        return ResponseEntity.ok().build();
    }
}
