import { Component, OnInit, inject } from '@angular/core';
import { Router, ActivatedRoute } from '@angular/router';
import { AuthService } from '../../_services/auth.service';


@Component({
  standalone: false,
  selector: 'app-login',
  templateUrl: './resetpassword.component.html',
  styleUrls: ['./resetpassword.component.css']
})
export class ResetPasswordComponent implements OnInit {
  private authService = inject(AuthService);
  route = inject(ActivatedRoute);
  router = inject(Router);

  form: any = {
    username: null,
    password: null
  };
  isSuccess = false;
  isFailed = false;
  tokenFailed = false;
  errorMessage = '';
  token: string = "";
  serverAnswer: string[] = [];

  ngOnInit(): void {
    let token = this.route.snapshot.paramMap.get('token');
    if (token != null) {
      this.token = token
    } else {
      this.tokenFailed = true;
    }
  }

  onSubmit(): void {
    const { username, password } = this.form;

    this.authService.changePassword(this.token, password).subscribe(
      data => {
        this.serverAnswer = data;
        this.router.navigate(['login']);
        this.isSuccess = true
      },
      err => {
        this.errorMessage = err.error.message;
        this.isFailed = true;
      }
    );
  }

}
