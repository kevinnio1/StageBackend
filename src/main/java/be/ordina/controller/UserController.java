package be.ordina.controller;

import be.ordina.model.AccountDTO;
import be.ordina.model.AccountCredentials;
import be.ordina.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Created by KeLe on 5/05/2017.
 */
@RestController
@RequestMapping(value = RequestMappings.USERS)
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private BlockchainController blockchainController;

    @RequestMapping(value="/register",method = RequestMethod.POST)
    public boolean register(@RequestBody final UserRegistration userRegistration) {
        AccountCredentials newUser = new AccountCredentials(userRegistration.getUsername(),userRegistration.getPassword());

        String wallID =  blockchainController.makeNewWallet(newUser.getPassword());
        newUser.setWalletID(wallID);
        newUser.setAdmin(false);
        boolean success = userService.createUser(newUser);
        if(success){
            try{
                return blockchainController.addNewNormalUser(newUser.getWalletID());
            }catch(Exception e){
                e.printStackTrace();
                return false;
            }
        }else {return false;}


    }

    @RequestMapping(value = "/isAdmin",method = RequestMethod.GET)
    public boolean isCurrentUserAdmin(){
        return userService.getCurrentUser().isAdmin();
    }

    @RequestMapping(value = "/getWalletID",method = RequestMethod.GET)
    public String getWalletIdCurrUser(){
        return "{\"walletID\":\"" + userService.getCurrentUser().getWalletID()+ "\"}";
    }

    public String getWalletIDcurrentUser(){
        return userService.getCurrentUser().getWalletID();
    }

    public String getWalletPassword() {

        return userService.getCurrentUser().getPassword();
    }

    public boolean currentUserIsAdmin() {

        return userService.getCurrentUser().isAdmin();
    }

    public boolean makeAdmin(String adminID) {

        return userService.makeAdminBywalletID(adminID);
    }

    public boolean removeAdmin(String adminID) {
        return userService.removeAdmin(adminID);
    }

    public List<AccountDTO> fillUsernamesWalletbalanceArray(List<AccountDTO> walletAndBalance) {
        return userService.fillUsernamesWalletbalanceArray(walletAndBalance);
    }


    public static class UserRegistration {
        String username;
        String password;

        public UserRegistration(String username, String password) {
            this.username = username;
            this.password = password;
        }

        public UserRegistration(String username, String password, String walletID) {
            this.username = username;
            this.password = password;
        }

        public UserRegistration() {
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

}
