package be.ordina.security;

import be.ordina.repository.IMongoModelEnabledRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * Created by KeLe on 9/05/2017.
 */
public class CustomerUserDetailsService implements UserDetailsService {

    @Autowired
    private IMongoModelEnabledRepository mongoClient;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
       AccountCredentials creds = mongoClient.findByUsername(username);
        if(creds!= null){

            return new User(creds.getUsername(),creds.getPassword(),true,true,true,true, AuthorityUtils.createAuthorityList("ADMIN"));

        }
        else {

            throw new UsernameNotFoundException("could not find the user '"
                    + username + "'");
        }


    }
}
