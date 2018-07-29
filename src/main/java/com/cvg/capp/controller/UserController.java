/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.cvg.capp.controller;

import com.cvg.capp.command.LoginCommand;
import com.cvg.capp.command.UserCommand;
import com.cvg.capp.domain.User;
import com.cvg.capp.exception.UserBlockedException;
import com.cvg.capp.service.UserService;
import javax.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 *Handles User related operations
 * @author ccarr
 */
@Controller
public class UserController {
    
    @Autowired
    private UserService userService;
    
    //giving value means you can add multiple mappings
    //"/" is the default mapping
    @RequestMapping(value = {"/", "/index"})
    public String index(Model m) {
        m.addAttribute("command", new LoginCommand());
        //index is the page name 
        //prefix is /web-inf/view
        //suffix is .jsp
        return "index"; //JSP - /WEB-INF/view/index.jsp
    }
    
    @RequestMapping(value = "/login", method = RequestMethod.POST)
    public String handleLogin(@ModelAttribute("command") LoginCommand cmd, Model m, HttpSession session) {
        try {
            User loggedInUser = userService.login(cmd.getLoginName(), cmd.getPassword());
            if (loggedInUser == null) {
                //FAILED
                //add error message and go back to login-form
                m.addAttribute("err", "Login Failed! Enter valid credentials.");
                return "index";//JSP - Login FORM
            } 
            else //SUCCESS
            //check the role and redirect to a appropriate dashboard 
            {
                if (loggedInUser.getRole().equals(UserService.ROLE_ADMIN)) {
                    //add user detail in session (assign session to logged in user)
                    addUserInSession(loggedInUser, session);
                    return "redirect:admin/dashboard";
                } 
                else if (loggedInUser.getRole().equals(UserService.ROLE_USER)) {
                    //add user detail in session (assign session to logged in user)
                    addUserInSession(loggedInUser, session);
                    return "redirect:user/dashboard";
                } 
                else {
                    //add error message and go back to login-form
                    m.addAttribute("err", "Invalid User ROLE");
                    return "index";//JSP - Login FORM
                }
            }
        } catch (UserBlockedException ex) {
            //add error message and go back to login-form
            m.addAttribute("err", ex.getMessage());
            return "index";//JSP - Login FORM
        }
    }
    
    @RequestMapping(value ="/logout")
    public String logout(HttpSession session) {
        session.invalidate();
      
        return "redirect:index?act=lo"; //JSP index with lo flag
    }
    
    @RequestMapping(value ="/user/dashboard")
    public String userDashboard(Model m, HttpSession session) {
        // ??todo, is there a better way than a service to show logged in user??
        Integer userId = (Integer) session.getAttribute("userId");
        //Shows logged in user
        m.addAttribute("userName", userService.getUserById(userId).getName());
        return "dashboard_user"; //JSP->dashboard_user.jsp
    }
    
    @RequestMapping(value ="/admin/dashboard")
    public String adminDashboard() {
        return "dashboard_admin"; //JSP->dashboard_admin.jsp
    }
    
    @RequestMapping(value = "/admin/users")
    public String getUserList(Model m) {
        m.addAttribute("userList", userService.getUserList());
        return "users"; //JSP
    }
    
    @RequestMapping(value = "/reg_form")
    public String registrationForm(Model m) {
        UserCommand cmd = new UserCommand();
        m.addAttribute("command", cmd);
        return "reg_form";//JSP
    }
    
    
    @RequestMapping(value = "/register")
    public String registerUser(@ModelAttribute("command") UserCommand cmd, Model m) {
        try {
            User user = cmd.getUser();
            user.setRole(UserService.ROLE_USER);
            user.setLoginStatus(UserService.LOGIN_STATUS_ACTIVE);
            userService.register(user);
            return "redirect:index?act=reg"; //Login Page
        } catch (DuplicateKeyException e) {
            e.printStackTrace();
            m.addAttribute("err", "Username is already registered. Please select another username.");
            return "reg_form";//JSP
        }
    }
    
    //binds user with a session
    private void addUserInSession(User u, HttpSession session) {
        session.setAttribute("user", u);
        session.setAttribute("userId", u.getUserId());
        session.setAttribute("role", u.getRole());
        
    }
    
    @RequestMapping(value = "/admin/change_status")
    @ResponseBody
    public String changeLoginStatus(@RequestParam Integer userId, @RequestParam Integer loginStatus) {
        try {
            userService.changeLoginStatus(userId, loginStatus);
            return "SUCCESS: Status Changed";
        } catch (Exception e) {
            e.printStackTrace();
            return "ERROR: Unable to Change Status";
        }
    }
}
