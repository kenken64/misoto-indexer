import Dexie, {Table} from 'dexie';
import { City } from '../model/city';

export class AppDB extends Dexie {

    cities!: Table<City, string>;
    
    constructor(){
        super('fileuploadApp');

        this.version(1).stores({
            cities: 'code, city_name'
        });
    }

    async addCity(item: City){
        await this.cities.put(item);
    }
}

export const db = new AppDB();