package com.driver.services.impl;

import com.driver.model.*;
import com.driver.repository.ConnectionRepository;
import com.driver.repository.ServiceProviderRepository;
import com.driver.repository.UserRepository;
import com.driver.services.ConnectionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ConnectionServiceImpl implements ConnectionService {
    @Autowired
    UserRepository userRepository2;
    @Autowired
    ServiceProviderRepository serviceProviderRepository2;
    @Autowired
    ConnectionRepository connectionRepository2;

    @Override
    public User connect(int userId, String countryName) throws Exception{
        User user=userRepository2.findById(userId).get();
        if(user.isConnected())throw new Exception("Already connected");
        String originalCountryName= String.valueOf(user.getCountry().getCountryName());
        if(countryName.equalsIgnoreCase(originalCountryName))return user;

        List<ServiceProvider> serviceProviderList=user.getServiceProviderList();
        if(serviceProviderList.isEmpty())throw new Exception("Unable to connect");

        int id=Integer.MAX_VALUE;
        ServiceProvider serviceProvider=null;
        Country country=null;
        for(ServiceProvider serviceProvider1:serviceProviderList){
            List<Country> countryList=serviceProvider1.getCountryList();
            for(Country country1:countryList){
                if(countryName.equalsIgnoreCase(country1.getCountryName().toString())){
                    if(serviceProvider1.getId()<id){
                        id=serviceProvider1.getId();
                        serviceProvider=serviceProvider1;
                        country=country1;
                    }
                }
            }
        }
        if(serviceProvider==null)throw new Exception("Unable to connect");
        Connection connection=new Connection();
        connection.setUser(user);
        connection.setServiceProvider(serviceProvider);
        user.setMaskedIp(country.getCode()+"."+serviceProvider.getId()+"."+user.getId());
        user.setConnected(true);
        user.getConnectionList().add(connection);
        serviceProvider.getConnectionList().add(connection);
        userRepository2.save(user);
        serviceProviderRepository2.save(serviceProvider);
        return user;
    }
    @Override
    public User disconnect(int userId) throws Exception {
        User user=userRepository2.findById(userId).get();
        if(!user.isConnected())throw new Exception("Already disconnected");
        user.setMaskedIp(null);
        user.setConnected(false);
        userRepository2.save(user);
        return user;
    }
    @Override
    public User communicate(int senderId, int receiverId) throws Exception {
        User sender=userRepository2.findById(senderId).get();

        User receiver=userRepository2.findById(receiverId).get();

        if(receiver.getMaskedIp()!=null){
            String maskedIp=receiver.getMaskedIp();

            String code= maskedIp.substring(0,3);

            code=code.toUpperCase();
            if(code.equals(sender.getCountry().getCode())){
                return sender;
            }
            String countryName=null;
            CountryName[] countryNames=CountryName.values();
            for(CountryName countryName1:countryNames){
                if(code.equals(countryName1.toCode())){
                    countryName=countryName1.toString();
                }
            }
            try{
                sender=connect(senderId,countryName);

            }
            catch (Exception e){
                throw new Exception("Cannot establish communication");
            }
            return sender;
        }
        if(sender.getCountry().getCountryName().equals(receiver.getCountry().getCountryName()))return sender;

        String receiverCountryName=receiver.getCountry().getCountryName().toString();
        sender=connect(senderId,receiverCountryName);
        if(!sender.isConnected())throw new Exception("Cannot establish communication");
        return sender;
    }
}
