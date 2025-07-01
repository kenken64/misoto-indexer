import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { FileuploadService } from '../services/fileupload.service';
import { Subscription } from 'rxjs';

@Component({
    selector: 'app-view-image',
    templateUrl: './view-image.component.html',
    styleUrl: './view-image.component.css',
    standalone: false
})
export class ViewImageComponent implements OnInit, OnDestroy{
  postId="";
  param$!: Subscription;
  imageData: any;
  comments!: string;

  private actRoute = inject(ActivatedRoute);
  private fileUploadSvc = inject(FileuploadService);
  private router = inject(Router);
  
  ngOnInit(): void {
    this.param$ = this.actRoute.params.subscribe(async(params) => {
      this.postId = params['postId'];
      let r = await this.fileUploadSvc.getImage(this.postId)
      this.imageData = r.image;
      this.comments = r.comments;
    });
  } 

  

  ngOnDestroy(): void {
      this.param$.unsubscribe();
  }

}
