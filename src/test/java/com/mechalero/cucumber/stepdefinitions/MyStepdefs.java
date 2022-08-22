package com.mechalero.cucumber.stepdefinitions;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Before;

import com.mechalero.cucumber.listener.Reporter;

import io.cucumber.datatable.DataTable;
import io.cucumber.java.Scenario;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class MyStepdefs {
	
	int a,b;

    @Before
    public void beforeScenario(Scenario scenario) {
        if (scenario.getName().equals("My First Scenario")) {
            Reporter.assignAuthor("Chris");
        }
    }

    @Given("I have {int} cukes in my belly") public void I_have_cukes_in_my_belly(int cukes)
        throws IOException {
        Reporter.addStepLog("My test addStepLog message");
        Reporter.addScenarioLog("This is scenario log");
        //        Reporter.addScreenCaptureFromPath(
        //            "/Users/vimalrajselvam/Downloads/best-resume-template-2016-3.jpg", "My title");
    }

    @Given("I have {int} cukes in my bellies") public void I_have_cukes_in_my_bellies(int cukes) {
    	Reporter.addStepLog("Cukes: "+cukes);
    }

    @Then("I print")
    public void i_print() {
    	Assert.fail("pailas");
    }
    
    @When("I login with credentials")
    public void i_login_with_credentials(DataTable dataTable) {
        throw new io.cucumber.java.PendingException();
    }

    @Given("a global administrator named {string}")
    public void a_global_administrator_named(String string) {
        Reporter.addStepLog(string);
    }

    @Given("I'm adding")
    public void i_m_adding() {
       
    }

    @When("I add {int} and {int}")
    public void i_add_and(Integer int1, Integer int2) {
        a = int1;
        b= int2;
    }

    @Then("the result is {int}")
    public void the_result_is(Integer int1) {
        Assert.assertTrue((a+b) == int1);
    }
}
