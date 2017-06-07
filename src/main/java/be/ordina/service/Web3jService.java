package be.ordina.service;

import be.ordina.model.AccountDTO;
import be.ordina.model.Vending;
import org.springframework.stereotype.Service;
import org.web3j.abi.FunctionEncoder;
import org.web3j.abi.datatypes.Address;
import org.web3j.abi.datatypes.Function;
import org.web3j.abi.datatypes.Type;
import org.web3j.abi.datatypes.generated.Int256;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.DefaultBlockParameterName;
import org.web3j.protocol.core.methods.request.Transaction;
import org.web3j.protocol.core.methods.response.*;
import org.web3j.protocol.http.HttpService;
import org.web3j.protocol.parity.Parity;
import org.web3j.protocol.parity.methods.response.PersonalUnlockAccount;
import org.web3j.tx.Contract;
import org.web3j.tx.ManagedTransaction;
import org.web3j.utils.Convert;
import rx.Subscription;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Created by KeLe on 2/05/2017.
 */


@Service
public class Web3jService {


    private Web3j web3; //defaults to http://localhost:8545
    private Credentials credentials;
    BigInteger gaslimit = BigInteger.valueOf(300000);
    Vending vendingContract;
    Parity parity;
    boolean minedTransaction = false;
    Subscription subscription;
    Subscription subscription1;
    Subscription subscription2;
    BigInteger duration = BigInteger.valueOf(3600);//one hour
    private List<String> connectedPeers = new ArrayList<>();
    private List<String> accountsArray;

    public Web3jService() throws IOException, CipherException, URISyntaxException {
        this.web3  = Web3j.build(new HttpService());
        this.parity = Parity.build(new HttpService());
        String url= System.getProperty( "user.dir" );
        String file = url.toString() + "/UTC--2017-05-17T12-51-15.921552827Z--64a17191e22a4034e7b119b2ecb6403533299312";
        this.credentials  = WalletUtils.loadCredentials(BlockchainLocalSettings.VENDING_PASSWORD,    file);
        vendingContract = Vending.load(BlockchainLocalSettings.VENDING_CONTRACT,web3,credentials, ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT);
        //subscribeToTransactionsandBlocks();
    }

    public void unsubscribeTransAndBlocks(){
        System.out.println("unsubscribed");
        subscription.unsubscribe();
        subscription1.unsubscribe();
        subscription2.unsubscribe();
    }

    public void subscribeToTransactionsandBlocks(){
        System.out.println("started subscription");
        //pending transactions
        subscription = web3.pendingTransactionObservable().subscribe(tx -> {
            System.out.println("is pending: " + tx.getHash());
        });

        //added to the blockchain
        subscription1 = web3.transactionObservable().subscribe(tx -> {
            System.out.println("added to the blockchain: " + tx.getHash());
        });

        subscription2 = web3.blockObservable(false).subscribe(block -> {
            for (EthBlock.TransactionResult transactionResult:
                    block.getBlock().getTransactions() ) {
                System.out.println("transaction in block equals?: " + transactionResult.get().hashCode());
            }
        });

    }

    public String getClientVersion() throws IOException, ExecutionException, InterruptedException {

        Web3ClientVersion web3ClientVersion = web3.web3ClientVersion().send();
        String clientVersion = web3ClientVersion.getWeb3ClientVersion();
        return clientVersion;

    }

    public List<String> getAccounts() throws IOException, ExecutionException, InterruptedException {
        List<String> list = new ArrayList<>();
        EthAccounts accounts =  web3.ethAccounts().send();
        for (String s : accounts.getAccounts()) {
            BigInteger balance = web3.ethGetBalance(s,DefaultBlockParameterName.LATEST).send().getBalance();
            String accAndBalance = s.concat("  ").concat(Convert.fromWei(balance.toString(), Convert.Unit.ETHER).toString()).concat("ETHER");
            list.add(accAndBalance);
        }
        return list;
    }


    public List<AccountDTO> getAccountsArray() throws IOException {
        EthAccounts accounts =  web3.ethAccounts().send();
        List<AccountDTO> accountDTOs = new ArrayList<>();
        for (String s : accounts.getAccounts()) {
            BigInteger balance = web3.ethGetBalance(s,DefaultBlockParameterName.LATEST).send().getBalance();
            AccountDTO acc = new AccountDTO(s,Convert.fromWei(balance.toString(), Convert.Unit.ETHER));
            accountDTOs.add(acc);
        }

        return accountDTOs;

    }

    public Integer getStock() throws IOException, ExecutionException, InterruptedException, CipherException {
        Type result = vendingContract.stock().get();
        return Integer.parseInt(result.getValue().toString());
    }

    public Integer vendingStockRefill(int amount,String currentwalletID, String passwordWallet) throws Exception {
        return doEthFunction(currentwalletID,passwordWallet,"stockup",amount);
    }

    public int doEthFunction(String currentwalletID,String passwordWallet, String func,int amountStockup) throws Exception {

        Function function=null;
        BigInteger ether = Convert.toWei("0.3", Convert.Unit.ETHER).toBigInteger();
        BigInteger am = BigInteger.valueOf(amountStockup);
        int stock = getStock();

        if(func.equalsIgnoreCase("pay")){
            if(stock==0) doReturn(func);

            function = new Function("pay", Arrays.asList(), Collections.emptyList());
            ether = Convert.toWei(String.valueOf(getPriceFinneyToEther()), Convert.Unit.ETHER).toBigInteger().add(Transaction.DEFAULT_GAS.multiply(gaslimit));
            BigDecimal accountBalance = Convert.fromWei(parity.ethGetBalance(currentwalletID,DefaultBlockParameterName.LATEST).send().getBalance().toString() , Convert.Unit.ETHER);
            //check if there is enough money in the wallet. Transaction would automaticly be discarded from the chain, but now we don't need to wait till transaction is verified.
            BigDecimal ethersend = new BigDecimal(ether);
            if(ethersend.compareTo(accountBalance)< 0 ){return getStock();}
        }else if(func.equalsIgnoreCase("stockup")) {
            if((getStock()+amountStockup)>getMaxStock()){return doReturn(func);}
            //no stock check needed because the blockchain smart contract has a max value + not enogh coins to buy more
            function = new Function("resupply", Arrays.asList(new Int256(am)), Collections.emptyList());
            ether = Convert.toWei("0.0", Convert.Unit.ETHER).toBigInteger();
        }else if (func.equalsIgnoreCase("setmin")){
            if(amountStockup >= getStock()){return doReturn(func);}
            function = new Function("setMinStock", Arrays.asList(new Int256(am)), Collections.emptyList());
            ether = Convert.toWei("0.0", Convert.Unit.ETHER).toBigInteger();
        }else if (func.equalsIgnoreCase("setmax")){
            if(amountStockup<getStock()){return doReturn(func);}
            function = new Function("setMaxStock", Arrays.asList(new Int256(am)), Collections.emptyList());
            ether = Convert.toWei("0.0", Convert.Unit.ETHER).toBigInteger();
        }
        //unlock accounts
        PersonalUnlockAccount currentacc = parity.personalUnlockAccount(currentwalletID,passwordWallet, duration).send();
        if(currentacc==null){
            throw new Exception("CurrentAccount is null!");
        }

        if (currentacc.accountUnlocked()) {
            //todo: check if balance 0, otherwise error
            if (web3.ethGetBalance(currentwalletID,DefaultBlockParameterName.LATEST).send().getBalance().doubleValue()<=0){return doReturn(func);}
            EthGetTransactionCount ethGetTransactionCount = web3.ethGetTransactionCount(currentwalletID, DefaultBlockParameterName.LATEST).sendAsync().get();
            BigInteger nonce = ethGetTransactionCount.getTransactionCount();
            String encodedFunction = FunctionEncoder.encode(function);
            org.web3j.protocol.core.methods.request.Transaction transaction = org.web3j.protocol.core.methods.request.Transaction.createFunctionCallTransaction(currentwalletID, nonce, Transaction.DEFAULT_GAS, gaslimit, BlockchainLocalSettings.VENDING_CONTRACT, ether, encodedFunction);
            org.web3j.protocol.core.methods.response.EthSendTransaction transactionResponse =parity.personalSignAndSendTransaction(transaction,passwordWallet).send();
            final String transactionHash = transactionResponse.getTransactionHash();
            if (transactionHash == null) {
                throw new Exception(transactionResponse.getError().getMessage());
            }
            EthGetTransactionReceipt transactionReceipt = null;
            //todo: indien niet toegevoegd door error moet deze niet wachten op de transactionreceipt. Dus een timeout hierop plaatsen?
            do {
                transactionReceipt = web3.ethGetTransactionReceipt(transactionHash).send();
            } while (!transactionReceipt.getTransactionReceipt().isPresent());

            return doReturn(func);
        }else{
            throw new Exception("account is locked");
        }


    }

    public int doReturn(String function) throws InterruptedException, ExecutionException, CipherException, IOException {
        if(function.equalsIgnoreCase("pay")){
            return getStock();
        }else if (function.equalsIgnoreCase("stockup")){
            return getStock();
        } else if(function.equalsIgnoreCase("setmin")){
            return getMinStock();
        } else if(function.equalsIgnoreCase("setmax")){
            return getMaxStock();
        } else {
            return 0;
        }
    }
    public BigDecimal getBalance(String walletIDcurrentUser) throws IOException, ExecutionException, InterruptedException {
        BigInteger balance = web3.ethGetBalance(walletIDcurrentUser,DefaultBlockParameterName.LATEST).send().getBalance();
        BigDecimal etherBalance =  Convert.fromWei(balance.toString(), Convert.Unit.ETHER);
        return etherBalance;
    }
    public int setMinStock(int amount, String currentwalletID, String passwordWallet) throws Exception {
        return doEthFunction(currentwalletID,passwordWallet,"setmin",amount);
    }
    public int setMaxStock(int amount, String currentwalletID, String passwordWallet) throws Exception {
        return doEthFunction(currentwalletID,passwordWallet,"setmax",amount);
    }

    public int buyOne(String currentwalletID,String passwordWallet) throws Exception {
        return doEthFunction(currentwalletID,passwordWallet,"pay",0);
    }

    public boolean addNewAdmin(String walletID) throws ExecutionException, InterruptedException {

        Address newAddress = new Address(walletID);
        TransactionReceipt transactionReceipt= vendingContract.addAdmin(newAddress).get();
        return true;
    }

    public boolean removeAdmin(String adminID) throws ExecutionException, InterruptedException {
        Address newAddress = new Address(adminID);
        TransactionReceipt transactionReceipt= vendingContract.removeAdmin(newAddress).get();
        return true;
    }

    public boolean addNewUser(String walletID) throws ExecutionException, InterruptedException {
        Address newAddress = new Address(walletID);
        //will wait till block is mined
        TransactionReceipt transactionReceipt= vendingContract.addUser(newAddress).get();
        return true;
    }

    public int getPercentStock() throws InterruptedException, ExecutionException, CipherException, IOException {

        Type result = vendingContract.maxStock().get();
        int maxStock = Integer.parseInt(result.getValue().toString());
        int currStock = getStock();
        double f = ((double)currStock / (double)maxStock) * 100;
        return (int)f;

    }

    public double getPriceFinneyToEther() throws ExecutionException, InterruptedException, IOException, CipherException {
        Type result = vendingContract.finneyPrice().get();
        return Double.parseDouble(result.getValue().toString())/1000;
    }
    public int getMaxStock() throws ExecutionException, InterruptedException {
        Type result = vendingContract.maxStock().get();
        return Integer.parseInt(result.getValue().toString());
    }
    public int getMinStock() throws ExecutionException, InterruptedException {
        Type result = vendingContract.minStock().get();
        return Integer.parseInt(result.getValue().toString());
    }


    public int getConnectedPeers() throws ExecutionException, InterruptedException, IOException {
        return web3.netPeerCount().send().getQuantity().intValue();
    }

    public String makeNewWallet(String pass) throws ExecutionException, InterruptedException, IOException {
        String res = parity.personalNewAccount(pass).send().getAccountId();
        return res;
    }


    public boolean setSupplier(String supplierID) throws ExecutionException, InterruptedException {
        Address supplierAddress = new Address(supplierID);
        TransactionReceipt tr =  vendingContract.setSupplier(supplierAddress).get();
        return true;
    }



}
