import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';

@Component({
    selector: 'app-backbutton',
    templateUrl: './backbutton.component.html',
    styleUrl: './backbutton.component.css',
    standalone: false
})
export class BackbuttonComponent {
  private router = inject(Router);

  goBack(){
    this.router.navigate(['']);
  }
}
