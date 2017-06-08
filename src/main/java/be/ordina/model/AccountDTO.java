package be.ordina.model;

import java.math.BigDecimal;

/**
 * Created by KeLe on 6/06/2017.
 */
public class AccountDTO {

    String username;
    String walletID;
    BigDecimal balance;

    public AccountDTO(String walletID, BigDecimal balance) {
        this.walletID = walletID;
        this.balance = balance;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getWalletID() {
        return walletID;
    }

    public void setWalletID(String walletID) {
        this.walletID = walletID;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }
}
