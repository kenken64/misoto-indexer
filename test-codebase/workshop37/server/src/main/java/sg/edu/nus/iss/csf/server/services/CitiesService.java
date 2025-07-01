package sg.edu.nus.iss.csf.server.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import sg.edu.nus.iss.csf.server.models.City;
import sg.edu.nus.iss.csf.server.repositories.CitiesRepositories;

@Service
public class CitiesService {
    @Autowired
    private CitiesRepositories citiesRepositories;

    public Optional<List<City>> getAllCities(){
        List<City> cc = this.citiesRepositories.getAllCities();
        if(cc!= null && !cc.isEmpty()){
            return Optional.of(cc);
        }   
        return Optional.empty();
    }
}
