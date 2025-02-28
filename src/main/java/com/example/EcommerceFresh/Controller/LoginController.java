package com.example.EcommerceFresh.Controller;


import com.example.EcommerceFresh.Dao.RoleDao;
import com.example.EcommerceFresh.Dao.UserDao;
import com.example.EcommerceFresh.Entity.Roles;
import com.example.EcommerceFresh.Entity.Users;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.Set;

@Controller
public class LoginController {
    @Autowired
    private BCryptPasswordEncoder bCryptPasswordEncoder;
    @Autowired
    UserDao userDao;
    @Autowired
    RoleDao roleDao;

    @GetMapping("/login")
    public String login(){
        return "login";
    }

    @GetMapping("/register")
    public String registerGet(Model model){
        model.addAttribute("user",new Users());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") Users users, RedirectAttributes redirectAttributes) {
        System.out.println("Register method called with email: " + users.getEmail());

        if(userDao.findByEmail(users.getEmail()).isPresent()){
            System.out.println("User Alrady Exists" +users.getEmail());
            return "redirect:/register";
        }


        users.setPassword(bCryptPasswordEncoder.encode(users.getPassword()));
        userDao.save(users);
        return "redirect:/login";


//        Roles userRole = roleDao.findByName("ROLE_USER");
//        if (userRole == null) {
//            redirectAttributes.addFlashAttribute("error", "Role not found in database");
//            return "redirect:/register";
//        }
//
//        Set<Roles> roles = new HashSet<>();
//        roles.add(userRole);
//        users.setRoles(roles);
//
//        userDao.save(users);
//        return "redirect:/login";
    }



}
