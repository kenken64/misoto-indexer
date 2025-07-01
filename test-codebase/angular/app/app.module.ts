import { NgModule } from '@angular/core';
import { BrowserModule } from '@angular/platform-browser';
import { CommonModule } from '@angular/common';

import { AppRoutingModule } from './app-routing.module';
import { AppComponent } from './app.component';
import { BrowserAnimationsModule } from '@angular/platform-browser/animations';

import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatInputModule } from '@angular/material/input';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatStepperModule } from '@angular/material/stepper';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { provideHttpClient, withInterceptorsFromDi } from '@angular/common/http';
import { MatDividerModule } from '@angular/material/divider';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatSelectModule } from '@angular/material/select';
import { MatOptionModule } from '@angular/material/core';
import { MatCardModule } from '@angular/material/card';
import { MatRippleModule } from '@angular/material/core';
import { MatDialogModule } from '@angular/material/dialog';
import { MatTableModule } from '@angular/material/table';
import { MatTabsModule } from '@angular/material/tabs';
import { MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltipModule } from '@angular/material/tooltip';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatChipsModule } from '@angular/material/chips';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatNativeDateModule } from '@angular/material/core';
import { MatRadioModule } from '@angular/material/radio';
import { MatExpansionModule } from '@angular/material/expansion';
import { DragDropModule } from '@angular/cdk/drag-drop';
import { TextFieldModule } from '@angular/cdk/text-field';
import { DatePipe, TitleCasePipe } from '@angular/common';
import { FormConfirmationComponent } from './form-confirmation/form-confirmation.component';
import { FormViewerComponent } from './form-viewer/form-viewer.component';
import { FormsListComponent } from './forms-list/forms-list.component';
import { FormEditorComponent } from './form-editor/form-editor.component';
import { EditTitleDialogComponent } from './forms-list/edit-title-dialog.component';
import { LoginComponent } from './auth/login/login.component';
import { RegisterComponent } from './auth/register/register.component';
import { DashboardComponent } from './dashboard/dashboard.component';
import { SideMenuComponent } from './side-menu/side-menu.component';
import { AuthService } from './auth/auth.service';
import { AuthGuard } from './auth/auth.guard';
import { FormsService } from './services/forms.service';
import { HeaderComponent } from './shared/header/header.component';
import { FormDataListComponent } from './form-data-list/form-data-list.component';
import { FormDataViewerComponent } from './form-data-viewer/form-data-viewer.component';
import { FormDataConfirmationComponent } from './form-data-confirmation/form-data-confirmation.component';
import { RecipientsComponent } from './recipients/recipients.component';
import { RecipientDialogComponent } from './recipient-dialog/recipient-dialog.component';
import { RecipientGroupDialogComponent } from './recipient-group-dialog/recipient-group-dialog.component';
import { DebugFormsComponent } from './debug-forms.component';
import { LandingComponent } from './landing/landing.component';
import { PublicFormComponent } from './public-form/public-form.component';
import { BlockchainService } from './services/blockchain.service';
import { FormVerificationService } from './services/form-verification.service';
import { AskDynaformComponent } from './ask-dynaform/ask-dynaform.component';
import { FireworksComponent } from './shared/fireworks/fireworks.component';
import { NotFoundComponent } from './not-found/not-found.component';
import { FormSaveConfirmationDialogComponent } from './form-save-confirmation-dialog/form-save-confirmation-dialog.component';
import { LanguageSelectorComponent } from './language-selector/language-selector.component';
import { SettingsComponent } from './settings/settings.component';
import { TranslatePipe } from './shared/translate.pipe';
import { BhutanNdiComponent } from './bhutan-ndi/bhutan-ndi.component';
import { NdiRegisterComponent } from './ndi-register/ndi-register.component';

@NgModule({
  declarations: [
    AppComponent,
    FormConfirmationComponent,
    FormViewerComponent,
    FormsListComponent,
    FormEditorComponent,
    EditTitleDialogComponent,
    LoginComponent,
    RegisterComponent,
    DashboardComponent,
    SideMenuComponent,
    HeaderComponent,
    FormDataListComponent,
    FormDataViewerComponent,
    FormDataConfirmationComponent,
    RecipientsComponent,
    RecipientDialogComponent,
    RecipientGroupDialogComponent,
    DebugFormsComponent,
    LandingComponent,
    PublicFormComponent,
    AskDynaformComponent,
    FireworksComponent,
    NotFoundComponent,
    FormSaveConfirmationDialogComponent,
    LanguageSelectorComponent,
    SettingsComponent,
    TranslatePipe,
    BhutanNdiComponent,
    NdiRegisterComponent
  ],
  imports: [
    BrowserModule,
    BrowserAnimationsModule,
    CommonModule,
    AppRoutingModule,
    MatStepperModule,
    FormsModule,
    ReactiveFormsModule,
    MatIconModule,
    MatButtonModule,
    MatInputModule,
    MatFormFieldModule,
    MatProgressSpinnerModule,
    MatDividerModule,
    MatCheckboxModule,
    MatSelectModule,
    MatOptionModule,
    MatCardModule,
    MatRippleModule,
    MatDialogModule,
    MatTableModule,
    MatTabsModule,
    MatSnackBarModule,
    MatTooltipModule,
    MatButtonToggleModule,
    MatSlideToggleModule,
    MatAutocompleteModule,
    MatChipsModule,
    MatDatepickerModule,
    MatNativeDateModule,
    MatRadioModule,
    MatExpansionModule,
    DragDropModule,
    TextFieldModule
  ],
  providers: [
    provideHttpClient(withInterceptorsFromDi()),
    AuthService,
    AuthGuard,
    DatePipe,
    TitleCasePipe,
    FormsService,
    BlockchainService,
    FormVerificationService
  ],
  bootstrap: [AppComponent]
})
export class AppModule { }
