import { Role } from "./roles";

export class LoginStatusResponse {

    constructor(public username:String,public roles:Array<Role>){

    }
}
