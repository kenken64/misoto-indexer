import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';
import { SseService } from './sse.service';
import { ChangeDetectorRef } from '@angular/core';

@Component({
  selector: 'app-music-player',
  standalone: false,
  templateUrl: './music-player.component.html',
  styleUrl: './music-player.component.css',
})
export class MusicPlayerComponent {
  audioReady = false;
  error: string | null = null;
  audioSrc: string | null = null;
  loading = false; // <--- NEW: Track loading state

  constructor(
    private http: HttpClient,
    private sseService: SseService,
    private cdr: ChangeDetectorRef
  ) {}

  startDownload(): void {
    this.loading = true; // <--- NEW: Show progress
    this.http.get('/api/download-music', { responseType: 'text' }).subscribe({
      next: () => this.startSse(),
      error: (err) => {
        this.error = err.message;
        this.loading = false; // <--- NEW: Hide progress on error
      },
    });
  }

  startSse(): void {
    this.sseService.getServerSentEvents('/api/sse').subscribe({
      next: (event) => {
        console.log('Event data type:', typeof event.data);
        console.log('First 50 characters:', event.data.substring(0, 50));

        try {
          let bytes: Uint8Array;

          // Check if it's Base64 (only contains Base64 characters)
          const base64Pattern = /^[A-Za-z0-9+/]*={0,2}$/;
          console.log(event.data);
          if (base64Pattern.test(event.data.replace(/\s/g, ''))) {
            console.log('Detected Base64 format');
            // Decode Base64
            const binaryString = atob(event.data);
            bytes = new Uint8Array(binaryString.length);
            for (let i = 0; i < binaryString.length; i++) {
              bytes[i] = binaryString.charCodeAt(i);
            }
          } else {
            console.log('Detected raw binary string format');
            // Handle raw binary string (what you showed earlier)
            bytes = new Uint8Array(event.data.length);
            for (let i = 0; i < event.data.length; i++) {
              bytes[i] = event.data.charCodeAt(i) & 0xff;
            }
          }

          console.log('Processed bytes length:', bytes.length);
          console.log('First few bytes:', Array.from(bytes.slice(0, 10)));

          // Create blob and URL
          const blob = new Blob([bytes], { type: 'audio/mpeg' });
          console.log('Blob size:', blob.size);
          console.log(blob);
          const url = URL.createObjectURL(blob);
          console.log('Created URL:', url);

          this.audioReady = true;
          // Force change detection
          this.cdr.detectChanges();
          this.loading = false; // <--- NEW: Hide progress
          setTimeout(() => {
            const audioPlayer = document.getElementById(
              'audioPlayer'
            ) as HTMLAudioElement;
            if (audioPlayer) {
              if (audioPlayer.src && audioPlayer.src.startsWith('blob:')) {
                URL.revokeObjectURL(audioPlayer.src);
              }
              console.log(url);
              audioPlayer.src = url;
              audioPlayer.load();
              console.log('inside timeout');
              audioPlayer.play().catch(console.error);
            }
          }, 0);
        } catch (error) {
          console.error('Error processing audio data:', error);
          console.error('❌ SSE Error:', error);
          this.loading = false; // <--- NEW: Hide progress
        }
      },
      error: (err) => {
        this.error = err.message;
        this.loading = false; // <--- NEW: Hide progress
      },
    });
  }

   refresh(): void {
    // Reset all relevant state, and optionally restart download/SSE
    if (this.audioSrc) {
      URL.revokeObjectURL(this.audioSrc);
      this.audioSrc = null;
    }
    this.audioReady = false;
    this.error = null;
  }

  // Clean up on destroy
  ngOnDestroy() {
    if (this.audioSrc) {
      URL.revokeObjectURL(this.audioSrc);
    }
  }
}
