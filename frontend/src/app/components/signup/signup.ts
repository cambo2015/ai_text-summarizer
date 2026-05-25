import { Component } from '@angular/core';
import { AuthService } from '../../services/auth-service';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { NgIf } from "@angular/common"
import { HttpErrorResponse } from '@angular/common/http';
import { Router, ActivatedRoute } from '@angular/router';
import { Spinner } from "../spinner/spinner";
@Component({
  selector: 'app-signup',
  imports: [NgIf, ReactiveFormsModule, Spinner],
  templateUrl: './signup.html',
  styleUrl: './signup.css'
})
export class Signup {

  signupFormGroup!: FormGroup;
  authError: string = '';
  successMessage: string = '';
  public loading: boolean = false;

  constructor(private authService: AuthService, private formBuilder: FormBuilder, private router: Router, private route: ActivatedRoute) {
  }

  ngOnInit() {
    //get form group
    this.signupFormGroup = this.formBuilder.group({
      username: new FormControl('',
        [Validators.required,
        Validators.pattern('^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,4}$')
        ]
      ),
      password: new FormControl('',
        [Validators.required,
          Validators.minLength(8),
          Validators.pattern('^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$'),
        ]
      )
    })
  }

  get username() { return this.signupFormGroup.get('username') }
  get password() { return this.signupFormGroup.get('password') }

  onSubmit() {
    this.successMessage = '';
    this.authError = '';
    console.log("handling the submit button");

    if (this.signupFormGroup.invalid) {
      this.signupFormGroup.markAllAsTouched();
      return;
    }
    this.loading = true;

    //get username from form
    let myusername = this.signupFormGroup.controls['username'].value;
    let mypassword = this.signupFormGroup.controls['password'].value;
    console.log(myusername, mypassword)
    //get password from form
    this.authService.signup(myusername, mypassword).subscribe({
      next: (response: string) => {
        console.log("SIGNUP RESPONSE:", response)
        this.successMessage = response || 'Signup successful! Please check your email for a confirmation link to complete the signup process.';
        //please check your email for a confirmation link to complete the signup process.
        this.loading = false;
      },
      error: (err: HttpErrorResponse) => {
        console.log("SIGNUP ERROR:", err);
        console.log(err.error.text);
        let message = 'An unexpected error occurred';

        switch (err.status) {
          case 200:
            message = err.error.text || 'Your account already exists.';
            break;
          case 409:
            message = message || 'Account already exists. Please log in.';
            break;

          case 400:
            message = message || 'Invalid signup data.';
            break;

          case 500:
            message = 'Server error. Please try again later.';
            break;
        }

        this.signupFormGroup.setErrors({ signupFailed: true });
        this.authError = message;
        this.loading = false;
      }
    })
  }
}