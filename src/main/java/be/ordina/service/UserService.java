package be.ordina.service;

import be.ordina.controller.UserController;
import be.ordina.repository.IMongoModelEnabledRepository;
import be.ordina.security.AccountCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Created by KeLe on 8/05/2017.
 */
@Service
public class UserService {

    @Autowired
    private IMongoModelEnabledRepository mongoRespository;

    public boolean createUser(AccountCredentials user){

        try {

        mongoRespository.save(user);
        return true;
        }catch(Exception e){
            e.printStackTrace();
            return false;
        }
    }
    public Authentication getAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
    public AccountCredentials getCurrentUser(){
        Authentication authentication = getAuthentication();
        if (authentication==null)return null;
        final AccountCredentials authenticatedUser = mongoRespository.findByUsername(authentication.getName());
        return authenticatedUser;
    }


    public boolean makeAdminBywalletID(String adminID) {
        return setAdmin(adminID,true);
    }

    public boolean removeAdmin(String adminID) {
        return setAdmin(adminID,false);
    }

    public boolean setAdmin(String adminID,boolean isAdmin){
        AccountCredentials acc =  mongoRespository.findAccountCredentialsByWalletID(adminID);
        if(acc!=null){
            acc.setAdmin(isAdmin);
            mongoRespository.save(acc);
            return true;
        }else {return false;}
    }
}
