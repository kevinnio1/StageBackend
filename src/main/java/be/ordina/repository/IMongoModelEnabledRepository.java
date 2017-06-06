package be.ordina.repository;

import be.ordina.model.AccountCredentials;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;


/**
 * Created by KeLe on 8/05/2017.
 */
@Repository
public interface IMongoModelEnabledRepository extends MongoRepository<AccountCredentials,String> {

    public AccountCredentials findByUsername(String s);
    public AccountCredentials findAccountCredentialsByWalletID(String walletID);
}
