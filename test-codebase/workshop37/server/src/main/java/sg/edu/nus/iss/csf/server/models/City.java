package sg.edu.nus.iss.csf.server.models;

import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;

import jakarta.json.Json;
import jakarta.json.JsonObject;

public class City implements Serializable{
    private String code;
    private String cityName;

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getCityName() {
        return cityName;
    }

    public void setCityName(String cityName) {
        this.cityName = cityName;
    }

    public static City populate(ResultSet rs) throws SQLException{
        City city = new City();
        city.setCode(rs.getString("code"));
        city.setCityName(rs.getString("city_name"));
        return city;
    }

    public JsonObject toJSON(){
        return Json.createObjectBuilder()
                .add("code", getCode())
                .add("city_name", getCityName())
                .build();
    }

}
