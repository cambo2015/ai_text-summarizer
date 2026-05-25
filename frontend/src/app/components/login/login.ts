import { Component } from '@angular/core';
import { AuthService } from '../../services/auth-service';
import { LoginResponse } from '../../common/login-response';
import { FormBuilder, FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import {NgIf} from "@angular/common"
import { HttpErrorResponse } from '@angular/common/http';
import { Router,ActivatedRoute } from '@angular/router';



@Component({
  selector: 'app-login',
  imports: [NgIf,ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {

  loginFormGroup!: FormGroup;
  authError:string = '';

  constructor(private authService:AuthService,private formBuilder:FormBuilder,private router:Router,private route:ActivatedRoute){

  }

  ngOnInit(){

    //get form group
    this.loginFormGroup = this.formBuilder.group({
      username: new FormControl('',
        [Validators.required, 
          Validators.pattern('^[a-z0-9._%+-]+@[a-z0-9.-]+\\.[a-z]{2,4}$')
        ]
      ),
      password:new FormControl('',
        [Validators.required,
          Validators.minLength(8),
          // Validators.pattern('^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$'),
        ]
      )
    })
  }

  get username(){return this.loginFormGroup.get('username')}
  get password(){return this.loginFormGroup.get('password')}

  onSubmit(){
    console.log("handling the submit button");

    if(this.loginFormGroup.invalid){
      this.loginFormGroup.markAllAsTouched();
      return;
    }

    //get username from form
    let myusername = this.loginFormGroup.controls['username'].value;
    let mypassword = this.loginFormGroup.controls['password'].value;
    console.log(myusername,mypassword)
    //get password from form
    this.authService.login(myusername,mypassword).subscribe({
      next: (response: LoginResponse) => {
          console.log("LOGIN RESPONSE:",response)
          this.authService.resetAuthState();
        
          const returnUrl = this.route.snapshot.queryParamMap.get('returnUrl') || '/dashboard';
          this.router.navigateByUrl(returnUrl);
        },
        error: (err:HttpErrorResponse) => {
          console.error('Login failed', err);
          if(err.status === 401){
            this.authError = 'Invalid email or password';
          }else{
            this.authError = 'Login failed. Please try again.'
          }
          this.loginFormGroup.setErrors({authFailed:true});
        }
    })
  }
}
