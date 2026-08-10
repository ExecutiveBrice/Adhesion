import { Component, Input } from "@angular/core";

@Component({
  standalone: false,
    selector: 'switch',
    templateUrl: './switch.html',
    styleUrls: ['./switch.css']
  })
  export class SwitchComponent  {

    @Input()
    input!: Boolean;
  }
