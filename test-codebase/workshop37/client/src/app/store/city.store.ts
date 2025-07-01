import { Injectable } from "@angular/core";
import { City } from "../model/city";
import { ComponentStore } from "@ngrx/component-store";
import { from, Observable, switchMap, tap } from "rxjs";
import { db } from '../shared/app.db';
import { liveQuery } from "dexie";

export interface CityState {
    citiesSlice: City[];
    loading: boolean;
}

@Injectable({
    providedIn: 'root'
})
export class CityStore extends ComponentStore<CityState> {
    constructor() {
        super({ citiesSlice: [], loading: false });
    }

    // Selector
    readonly cities$ = this.select(state => state.citiesSlice);
    readonly loading$ = this.select(state => state.loading); //optional

    // Updaters
    readonly setLoading = this.updater((state, loading: boolean) => ({ ...state, loading })); //optional
    readonly setCities = this.updater<City[]>(
        (state, cities: City[]) => ({ ...state, cities })); // needed 

    // Effects
    readonly loadCities = this.effect((trigger$: Observable<void>) =>
        trigger$.pipe(
            tap(() => this.setLoading(true)),
            switchMap(() =>
                from(liveQuery(() => db.cities.reverse().toArray())).pipe(
                    tap({
                        next: (cities) => this.setCities(cities),
                        error: (error) => this.setLoading(false)
                    })
                )
            )
        )
    );

    //add new city
    readonly addNewCity = this.effect((city$: Observable<City>) =>
        city$.pipe(
            switchMap((city) =>
                from(db.addCity(city)).pipe(
                    tap(() => this.loadCities())
                )
            )
        )
    );

}