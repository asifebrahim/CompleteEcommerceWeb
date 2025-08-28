package com.example.EcommerceFresh.Controller;


import com.example.EcommerceFresh.Dao.RoleDao;
import com.example.EcommerceFresh.Dao.UserDao;
import com.example.EcommerceFresh.Dao.UserRoleDao;
import com.example.EcommerceFresh.Entity.Roles;
import com.example.EcommerceFresh.Entity.UserRole;
import com.example.EcommerceFresh.Entity.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

@Controller
public class LoginController {
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    UserDao userDao;
    @Autowired
    RoleDao roleDao;
    @Autowired
    UserRoleDao userRoleDao;

    @GetMapping("/login")
    public String login(){
        return "login";
    }
    @GetMapping("/WaitingMessage")
    public String WaitingMessage(Model model){
        return "WaitingMessage";
    }
    @GetMapping("/register")
    public String registerGet(Model model){
        model.addAttribute("user",new Users());
        return "register";
    }


    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") Users users) {
        System.out.println("Register method called with email: " + users.getEmail());

        if(userDao.findByEmail(users.getEmail()).isPresent()){
            System.out.println("User Already Exists: " + users.getEmail());
            return "redirect:/register";
        }

        // Encrypt the password
        users.setPassword(bCryptPasswordEncoder.encode(users.getPassword()));

        // Get and set the user role
        System.out.println("User id is : "+users.getId());
        Roles userRole = roleDao.findByName("ROLE_USER");
        if (userRole == null) {
            System.out.println("ROLE_USER not found, creating it...");
            userRole = new Roles();
            userRole.setName("ROLE_USER");
            userRole = roleDao.save(userRole);
        }

        // Create and assign roles set
        Set<Roles> roles = new HashSet<>();
        roles.add(userRole);
        users.setRoles(roles);
        userDao.save(users);
        return "redirect:/login";
    }


    @GetMapping("/forgotpassword")
    public String forgotPassword(Model model){
        model.addAttribute("resetPassword",new Users());
        return "forgotPasswordPage";
    }

    @PostMapping("/resetPassword")
    public String finalResetSubmission(@ModelAttribute("resetPassword") Users user) {
        String emailOfTheUser = user.getEmail();
        System.out.println("The email is: " + emailOfTheUser);

        Optional<Users> userOptional = userDao.findByEmail(emailOfTheUser);

        if (userOptional.isPresent()) {
            System.out.println("Email exists, sending reset link...");
           return "flashSucessResetPassword";
        } else {
            System.out.println("Wrong email");
        }
        return "redirect:/login";
    }



}
