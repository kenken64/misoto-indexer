import { Component, OnInit, OnDestroy } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { NdiService } from '../services/ndi.service';
import { Subscription } from 'rxjs';

@Component({
  selector: 'app-bhutan-ndi',
  templateUrl: './bhutan-ndi.component.html',
  styleUrl: './bhutan-ndi.component.css'
})
export class BhutanNdiComponent implements OnInit, OnDestroy {
  isLoading = false;
  errorMessage = '';
  qrCodeUrl = '';
  threadId = '';
  isListening = false;
  returnUrl = '';
  
  private sseSubscription?: Subscription;

  constructor(
    private ndiService: NdiService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit(): void {
    // Get return url from route parameters or default to dashboard
    this.returnUrl = this.route.snapshot.queryParams['returnUrl'] || '/dashboard';
    
    // Automatically create proof request when component loads
    this.createProofRequest();
  }

  ngOnDestroy(): void {
    this.stopSSEListening();
  }

  async createProofRequest(): Promise<void> {
    if (this.isLoading) return;

    this.isLoading = true;
    this.errorMessage = '';

    try {
      console.log('Creating NDI proof request...');
      
      const response = await this.ndiService.createProofRequest().toPromise();
      
      if (response && response.success && response.url) {
        console.log('NDI proof request created:', response);
        
        // Generate QR code for the proof request URL
        this.qrCodeUrl = this.ndiService.generateQRCodeUrl(response.url);
        this.threadId = response.threadId;
        
        console.log('QR Code URL:', this.qrCodeUrl);
        
        // Start listening for SSE notifications
        this.startSSEListening();
      } else {
        throw new Error('Invalid response from NDI service');
      }
    } catch (error: any) {
      console.error('NDI proof request error:', error);
      this.errorMessage = 'Failed to create NDI verification request. Please try again.';
    } finally {
      this.isLoading = false;
    }
  }

  private startSSEListening(): void {
    if (this.isListening) return;
    
    this.isListening = true;
    console.log('Starting SSE connection for NDI verification...');

    this.sseSubscription = this.ndiService.createSSEConnection(this.threadId).subscribe({
      next: (event) => {
        console.log('📡 SSE Event received:', event);
        console.log('📡 Event type:', event.type);
        console.log('📡 Event timestamp:', new Date().toISOString());
        
        if (event.type === 'ndi-verification') {
          console.log('🎯 NDI verification event detected');
          console.log('📋 Event data structure:', JSON.stringify(event.data, null, 2));
          
          // Additional payload validation logging
          if (event.data) {
            console.log('✅ Event has data object');
            console.log('📊 Data success flag:', event.data.success);
            console.log('📊 Data message:', event.data.message);
            console.log('📊 Data analysis:', event.data.analysis);
            console.log('📊 Data NDI payload:', event.data.data);
          } else {
            console.log('❌ Event missing data object');
          }
          
          this.onVerificationSuccess(event.data);
        } else if (event.type === 'connected') {
          console.log('🔗 SSE connection established successfully');
        } else if (event.type === 'heartbeat') {
          console.log('💓 SSE heartbeat received - connection alive');
        } else {
          console.log('❓ Unknown SSE event type:', event.type);
        }
      },
      error: (error) => {
        console.error('❌ SSE connection error:', error);
        console.error('❌ Error details:', JSON.stringify(error, null, 2));
        this.errorMessage = 'Connection lost. Please try again.';
        this.isListening = false;
      }
    });
  }

  private stopSSEListening(): void {
    if (this.sseSubscription) {
      this.sseSubscription.unsubscribe();
      this.sseSubscription = undefined;
    }
    this.isListening = false;
    console.log('SSE connection stopped');
  }

  private onVerificationSuccess(proof: any): void {
    // Handle successful verification
    console.log('🔄 NDI verification processing started:', proof);
    
    // Stop SSE listening since verification is complete
    this.stopSSEListening();
    
    // Multiple validation checks for robust navigation
    const validationResults = this.validateNDIProof(proof);
    console.log('🔍 NDI validation results:', validationResults);
    
    if (validationResults.isValid) {
      console.log('✅ NDI verification validated - Processing user data');
      
      // Extract user data from the requested_presentation
      const userData = this.extractUserDataFromProof(proof);
      console.log('👤 Extracted user data:', userData);
      
      if (userData.idNumber && userData.fullName) {
        console.log('📝 Complete user data extracted, navigating to registration');
        
        // Navigate to registration form with pre-filled data
        this.navigateToRegister(proof, userData, 'complete-data');
      } else {
        console.log('⚠️ Incomplete user data, proceeding to manual registration');
        
        // Navigate to registration form with verification data
        this.navigateToRegister(proof, userData, 'incomplete-data');
      }
    } else {
      console.log('❌ NDI verification validation failed, attempting fallback navigation');
      console.log('❌ Validation failure reasons:', validationResults.reasons);
      
      // Fallback: still navigate but with warning
      this.navigateToRegister(proof, null, 'fallback');
    }
  }

  private validateNDIProof(proof: any): {isValid: boolean, reasons: string[]} {
    const reasons: string[] = [];
    // Strictly require verification_result === 'ProofValidated'
    const verificationResult = proof?.verification_result || proof?.data?.verification_result;
    const isProofValidated = verificationResult === 'ProofValidated';
    if (!isProofValidated) {
      reasons.push(`verification_result is not 'ProofValidated' (actual: '${verificationResult}')`);
    }
    // Optionally, you can keep other checks for logging or debugging
    // But only ProofValidated is considered valid
    return { isValid: isProofValidated, reasons };
  }

  private navigateToRegister(proof: any, userData: any, reason: string): void {
    console.log(`🧭 Navigating to registration - Reason: ${reason}`);
    console.log(proof);
    console.log(userData);
    
    try {
      this.router.navigate(['/ndi-register'], { 
        state: { 
          ndiData: proof,
          userData: userData,
          navigationReason: reason
        },
        queryParams: { returnUrl: this.returnUrl }
      });
      
      console.log('✅ Navigation to registration initiated successfully');
    } catch (error) {
      console.error('❌ Navigation to registration failed:', error);
      this.errorMessage = 'Navigation failed. Please try again.';
    }
  }

  private extractUserDataFromProof(proof: any): {idNumber: string, fullName: string, email: string} {
    let idNumber = '';
    let fullName = '';
    let email = '';

    try {
      console.log('🔍 Starting user data extraction from proof...');
      
      // Try to extract from requested_presentation (new NDI format)
      const presentation = proof?.data?.data?.requested_presentation;
      console.log('📋 Presentation object:', presentation);
      
      if (presentation?.revealed_attrs) {
        console.log('✅ Found revealed_attrs:', presentation.revealed_attrs);
        
        // Handle the new structure where attributes are keyed by name
        // Example: "Full Name": [{"value": "Dorji Sonam", "identifier_index": 0}]
        
        // Extract Full Name
        if (presentation.revealed_attrs['Full Name']) {
          const fullNameArray = presentation.revealed_attrs['Full Name'];
          if (Array.isArray(fullNameArray) && fullNameArray.length > 0 && fullNameArray[0].value) {
            fullName = fullNameArray[0].value;
            console.log('✅ Extracted Full Name:', fullName);
          }
        }
        
        // Extract ID Number
        if (presentation.revealed_attrs['ID Number']) {
          const idNumberArray = presentation.revealed_attrs['ID Number'];
          if (Array.isArray(idNumberArray) && idNumberArray.length > 0 && idNumberArray[0].value) {
            idNumber = idNumberArray[0].value;
            console.log('✅ Extracted ID Number:', idNumber);
          }
        }
      } else {
        console.log('❌ No revealed_attrs found in presentation');
        
        // Fallback: Try the old structure
        const oldPresentation = proof?.data?.requested_presentation;
        if (oldPresentation?.revealed_attrs) {
          console.log('🔄 Trying fallback extraction from old structure');
          Object.values(oldPresentation.revealed_attrs).forEach((attr: any) => {
            if (attr?.raw) {
              // Check if this looks like an ID number (numeric)
              if (/^\d+$/.test(attr.raw) && attr.raw.length >= 3) {
                idNumber = attr.raw;
              }
              // Check if this looks like a name (contains letters and spaces)
              else if (/^[a-zA-Z\s]+$/.test(attr.raw) && attr.raw.length > 2) {
                fullName = attr.raw;
              }
            }
          });
        }
      }

      console.log('📊 Final extracted user data:', { idNumber, fullName, email });
    } catch (error) {
      console.error('❌ Error extracting user data from proof:', error);
    }

    return { idNumber, fullName, email };
  }

  // Retry verification
  retry(): void {
    this.stopSSEListening();
    this.qrCodeUrl = '';
    this.threadId = '';
    this.errorMessage = '';
    this.createProofRequest();
  }

  // Cancel and go back to login
  cancel(): void {
    this.stopSSEListening();
    this.router.navigate(['/login'], { 
      queryParams: { returnUrl: this.returnUrl } 
    });
  }

  // Navigate to home
  goToHome(): void {
    this.stopSSEListening();
    this.router.navigate(['/']);
  }

  // Handle QR code image loading error
  onQRError(): void {
    console.error('QR Code loading failed for URL:', this.qrCodeUrl);
    console.error('This could be due to CSP restrictions or network issues');
    this.errorMessage = 'Failed to load QR code. Please try again or check your network connection.';
  }
}
