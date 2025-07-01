package sg.edu.nus.iss.csf.server.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import sg.edu.nus.iss.csf.server.models.City;
import sg.edu.nus.iss.csf.server.services.CitiesService;

@Controller
public class CitiesController {
    @Autowired
    private CitiesService citiesService;

    @GetMapping(path="/api/cities", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getCities(){
        JsonArray result = null;
        Optional<List<City>> cc = this.citiesService.getAllCities();
        JsonArrayBuilder jab = Json.createArrayBuilder();
        cc.get().forEach(c -> {
            jab.add(c.toJSON());
        });
        result = jab.build();
        
        return ResponseEntity
                .status(HttpStatus.OK)
                .contentType(MediaType.APPLICATION_JSON)
                .body(result.toString());
    }
}
