package controllers;

import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class HelloWorldController extends Controller {

    public Result index() {
        return ok("Hello World");
    }
}
