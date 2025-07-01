import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { UploadComponent } from './components/upload.component';
import { ViewImageComponent } from './components/view-image.component';
import { GalleryComponent } from './components/gallery.component';

const routes: Routes = [
  {path:"", component: UploadComponent},
  {path:"gallery", component: GalleryComponent},
  {path:"image/:postId", component: ViewImageComponent},
  {path:"**", redirectTo:"/", pathMatch:"full"}
];

@NgModule({
  imports: [RouterModule.forRoot(routes)],
  exports: [RouterModule]
})
export class AppRoutingModule { }
