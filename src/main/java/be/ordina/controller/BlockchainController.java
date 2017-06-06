package be.ordina.controller;

import be.ordina.service.Web3jService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.web3j.crypto.CipherException;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by KeLe on 27/04/2017.
 */
@CrossOrigin
@RestController
@RequestMapping(value = RequestMappings.BLOCKCHAIN)
public class BlockchainController {

    @Autowired
    private UserController userController;

    @Autowired
    private Web3jService web3jService;

    @RequestMapping(value="/getClientVersion",method = RequestMethod.GET)
    public String getClientVersion() {
        String res = "";
        try {
            res = web3jService.getClientVersion();
        return res;
        } catch (Exception e) {
            e.printStackTrace();
            return "An error has occurred.";
        }
    }

    @RequestMapping(value="/getAccounts",method = RequestMethod.GET)
    public List<String> getAccounts() {
        List<String> res = new ArrayList<>();
        try {
            if(userController.currentUserIsAdmin()){
            res = web3jService.getAccounts();}else {return new ArrayList<>();}
            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    @RequestMapping(value="/getPeerCount",method = RequestMethod.GET)
    public int getPeerCount() {
        try {
            return web3jService.getConnectedPeers();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }


    @RequestMapping(value="/getStock",method = RequestMethod.GET)
    public int getSTock() {
        int res = 0;

        try {
            res = web3jService.getStock();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    @RequestMapping(value="/stockRefill/{amount}",method = RequestMethod.POST)
    public int stockRefill(@PathVariable String amount) {
        int res = 0;

        try {
            //check if it is an admin
            boolean isAdmin = userController.currentUserIsAdmin();
            if(isAdmin){
            String currentwalletID = userController.getWalletIDcurrentUser();
            String passwordWallet = userController.getWalletPassword();
            res = web3jService.vendingStockRefill( Integer.parseInt(amount),currentwalletID,passwordWallet);
            }else {
                res = getSTock();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return getSTock();
        }
        return res;
    }
    
    @RequestMapping(value="/buyOne",method = RequestMethod.POST)
    public int buyOne() {
        int res = 0;
        try {
            String currentwalletID = userController.getWalletIDcurrentUser();
            String passwordWallet = userController.getWalletPassword();
            res = web3jService.buyOne(currentwalletID,passwordWallet);
        } catch (Exception e) {
            e.printStackTrace();
            return getSTock();

        }
        return res;
    }


    @RequestMapping(value="/getPercentStock",method = RequestMethod.GET)
    public int getPercentStock(){
        try {
            return web3jService.getPercentStock();
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public boolean addNewNormalUser(String walletID){
        try {
            return web3jService.addNewUser(walletID);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    @RequestMapping(value="/getMaxStock",method = RequestMethod.GET)
    public int getMaxStock() {
        int res = 0;

        try {
            res = web3jService.getMaxStock();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }

    @RequestMapping(value="/getBalanceCurrUser",method = RequestMethod.GET)
    public Float getBalanceCurrUser() {
        BigDecimal res=new BigDecimal("0.00");
        try {
            res = web3jService.getBalance(userController.getWalletIDcurrentUser());

        } catch (Exception e) {
            e.printStackTrace();
        }
        return res.floatValue();
    }

    @RequestMapping(value="/getMinStock",method = RequestMethod.GET)
    public int getMinStock() {
        int res = 0;
        try {
            res = web3jService.getMinStock();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return res;
    }



    @RequestMapping(value="/setMinStock/{amount}",method = RequestMethod.POST)
    public int setMinStock(@PathVariable String amount) {
        int res = 0;

        try {
            //check if it is an admin
            boolean isAdmin = userController.currentUserIsAdmin();
            if(isAdmin){
                String currentwalletID = userController.getWalletIDcurrentUser();
                String passwordWallet = userController.getWalletPassword();
                res = web3jService.setMinStock(Integer.parseInt(amount),currentwalletID,passwordWallet);
            }else {
                res = getMinStock();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return getMinStock();
        }
        return res;
    }

    @RequestMapping(value="/setMaxStock/{amount}",method = RequestMethod.POST)
    public int setMaxStock(@PathVariable String amount) {
        int res = 0;

        try {
            //check if it is an admin
            boolean isAdmin = userController.currentUserIsAdmin();
            if(isAdmin){
                String currentwalletID = userController.getWalletIDcurrentUser();
                String passwordWallet = userController.getWalletPassword();
                res = web3jService.setMaxStock(Integer.parseInt(amount),currentwalletID,passwordWallet);
                return res;
            }else {
                return getMaxStock();
            }
        } catch (Exception e) {
            e.printStackTrace();
            return getMaxStock();
        }
    }


    public String makeNewWallet(String pass) {

        try {
            return web3jService.makeNewWallet(pass);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

    }

    @RequestMapping(value="/setSupplier/{supplierID}",method = RequestMethod.POST)
    public boolean setSupplier(@PathVariable String supplierID) {
        boolean res;

        try {
            //check if it is an admin
            boolean isAdmin = userController.currentUserIsAdmin();
            if (isAdmin) {
                res = web3jService.setSupplier(supplierID);
                return res;
            } else {
                return false;
            }
      } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    @RequestMapping(value="/addAdmin/{adminID}", method=RequestMethod.POST)
    public boolean addNewAdmin(@PathVariable String adminID){

        try {
            userController.makeAdmin(adminID);
            return web3jService.addNewAdmin(adminID);
        }catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    @RequestMapping(value="/removeAdmin/{adminID}", method=RequestMethod.POST)
    public boolean removeAdmin(@PathVariable String adminID){

        try {
            userController.removeAdmin(adminID);
            return web3jService.removeAdmin(adminID);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }




}
