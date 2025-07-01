import { Component, inject, OnInit } from '@angular/core';
import { CityStore } from '../store/city.store';
import { Observable } from 'rxjs';
import { City } from '../model/city';

@Component({
  selector: 'app-gallery',
  standalone: false,
  templateUrl: './gallery.component.html',
  styleUrl: './gallery.component.css'
})
export class GalleryComponent implements OnInit{
  private cityStore = inject(CityStore);

  citiesList$!: Observable<City[]>;
  selectedCity! : string;
  selectedCityName? : string;
  

  ngOnInit(): void {
    this.loadCities();
  }

  loadCities(){
    this.citiesList$! = this.cityStore.cities$;
    this.cityStore.loadCities();
  }
}
