import { Injectable } from '@angular/core';
import { LacunaWebPKI, CertificateModel } from 'web-pki';
import { BehaviorSubject, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class WebPkiService {
  /**
   * WebPKI lib. Needs to be loaded.
   */
  private pki: LacunaWebPKI;

  /**
   * Certificate list.
   */
  private certificatesSubject = new BehaviorSubject<CertificateModel[]>([]);
  
  constructor() {
    this.pki = new LacunaWebPKI();
  }

  /**
   * Returns the certificate list subject as observable.
   * @returns 
   */
  getCertificates(): Observable<CertificateModel[]> {
    return this.certificatesSubject.asObservable();
  }

  /**
   * Initiates the WebPKI lib and then list all certificates found
   * in the user computer.
   */
  async initialize(): Promise<void> {
    try {
      this.pki.init(({ready: () => {
        this.pki.listCertificates().success((certificates) => {
          this.certificatesSubject.next(certificates);
        })
      }}));
    } catch (error) {
      console.error('Error initializing WebPKI:', error);
      throw error;
    }
  }

  /**
   * Using the given certificate, signs the pdf hash bytes. The return
   * is the signature hash and not the signed document itself.
   *
   * @param hash To be signed hash. Can either be the whole document bytes or
   * just a specific part (dependending on the API).
   * @param thumbprint The selected certificate thumbprint.
   *
   * @returns Signature hash in base64 format.
   */
  signHash(hash: string, thumbprint: string, hashAlgorithm: string): Promise<string> {
    return new Promise((resolve, reject) => {
      this.pki.signData({
        thumbprint,
        data: hash,
        digestAlgorithm: hashAlgorithm,
      }).success(signature => {
        resolve(signature);
      }).fail(error => {
        console.error('Error signing hash:', error);
        reject(error);
      });
    });
  }

  /**
   * Reads the selected certificate and returns its base64-encoded content.
   * @param thumbprint The thumbprint of the selected certificate.
   */
  readCertificate(thumbprint: string): Promise<string> {
    return new Promise((resolve, reject) => {
      this.pki.readCertificate({
        thumbprint
      })
        .success((content) => resolve(content))
        .fail((error) => reject(error))
    })
  }
}