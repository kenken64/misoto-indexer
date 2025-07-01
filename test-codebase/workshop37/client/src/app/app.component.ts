import { Component, inject, OnInit } from '@angular/core';
import { CitiesService } from './services/cities.service';
import { CityStore } from './store/city.store';
import { City } from './model/city';

@Component({
    selector: 'app-root',
    templateUrl: './app.component.html',
    styleUrl: './app.component.css',
    standalone: false
})
export class AppComponent implements OnInit {
  title = 'client';
  cities!: City[];
  private citiesSvc = inject(CitiesService);
  private citiesStore = inject(CityStore);
  
  async ngOnInit() {
    this.cities = await this.citiesSvc.getCities();
    this.cities.forEach((cityObj) => {
      console.log(cityObj);
      console.log(cityObj.code);
      console.log(cityObj.city_name);
      this.citiesStore.addNewCity(cityObj);
    });
  }
}
