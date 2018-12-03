package controllers;

import dtos.netservice.*;
import managers.InvalidArgumentException;
import managers.UnauthorizedException;
import managers.netservicemanager.NetServiceManager;
import managers.netservicemanager.PlaintextCredential;
import models.NetService;
import models.NetServiceCredential;
import play.data.Form;
import play.data.FormFactory;
import play.libs.Json;
import play.mvc.Result;
import policyenforcement.session.Authentication;
import views.html.netservice.CreateNetService;
import views.html.netservice.CreateNetServiceCredential;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static play.libs.Scala.asScala;
import static play.mvc.Results.*;

@Singleton
@Authentication.Required
public class NetServiceController {

    private final NetServiceManager netServiceManager;
    private final Form<CreateNetServiceDto> createNetServiceDtoForm;
    private final Form<DeleteNetServiceDto> deleteNetServiceDtoForm;
    private final Form<AddNetServiceParameterDto> addNetServiceParameterDtoForm;
    private final Form<CreateNetServiceCredentialsDto> createNetServiceCredentialsDtoForm;
    private final Form<DeleteNetServiceCredentialsDto> deleteNetServiceCredentialsDtoForm;

    @Inject
    public NetServiceController(NetServiceManager netServiceManager, FormFactory formFactory){
        this.netServiceManager = netServiceManager;
        this.createNetServiceDtoForm = formFactory.form(CreateNetServiceDto.class);
        this.deleteNetServiceDtoForm = formFactory.form(DeleteNetServiceDto.class);
        this.addNetServiceParameterDtoForm = formFactory.form(AddNetServiceParameterDto.class);
        this.createNetServiceCredentialsDtoForm = formFactory.form(CreateNetServiceCredentialsDto.class);
        this.deleteNetServiceCredentialsDtoForm = formFactory.form(DeleteNetServiceCredentialsDto.class);
    }



    public Result showAllNetServices() throws UnauthorizedException {
        List<NetService> netServices = netServiceManager.getAllNetServices();
        return ok(views.html.netservice.NetServices.render(asScala(netServices)));
    }
    
    
    public Result deleteNetService() throws UnauthorizedException, InvalidArgumentException {
        Form<DeleteNetServiceDto> boundForm = deleteNetServiceDtoForm.bindFromRequest();

        if(boundForm.hasErrors()){
            return redirect(routes.NetServiceController.showAllNetServices());
        }

        netServiceManager.deleteNetService(boundForm.get().getNetServiceId());
        return redirect(routes.NetServiceController.showAllNetServices());
    }
    
    public Result showAddNetServiceForm(){
        return ok(views.html.netservice.CreateNetService.render(createNetServiceDtoForm));
    }
    
    public Result createNetService() throws UnauthorizedException {
        Form<CreateNetServiceDto> boundForm = createNetServiceDtoForm.bindFromRequest();

        if(boundForm.hasErrors())
        {
            return badRequest(views.html.netservice.CreateNetService.render(boundForm));
        }

        CreateNetServiceDto dto = boundForm.get();
        netServiceManager.createNetService(dto.getName(), dto.getUrl());
        return redirect(routes.NetServiceController.showAllNetServices());
    }

    public Result showEditNetService(Long netServiceId) throws UnauthorizedException {
        Optional<NetService> netService = netServiceManager.getNetService(netServiceId);

        if(!netService.isPresent())
            return badRequest();


        AddNetServiceParameterDto addNetServiceParameterDto = new AddNetServiceParameterDto();
        addNetServiceParameterDto.setNetServiceId(netServiceId);
        Form<AddNetServiceParameterDto> filledForm = addNetServiceParameterDtoForm.fill(addNetServiceParameterDto);

        return ok(views.html.netservice.EditNetService.render(netService.get(), filledForm));
    }

    public Result addNetServiceParameter() throws UnauthorizedException, InvalidArgumentException {
        Form<AddNetServiceParameterDto> boundForm = addNetServiceParameterDtoForm.bindFromRequest();

        if(boundForm.hasErrors()) {
            return badRequest();
        }

        netServiceManager.addNetServiceParameter(boundForm.get().getNetServiceId(), boundForm.get().getName(), boundForm.get().getDefaultValue());
        return redirect(routes.NetServiceController.showEditNetService(boundForm.get().getNetServiceId()));

    }


    public Result showUserNetServiceCredentials(){

        List<NetServiceCredential> credentials = netServiceManager.getUserNetServiceCredentials();
        return ok(views.html.netservice.NetServiceCredentials.render(asScala(credentials)));
    }
    public Result showCreateNetServiceCredentialForm() throws UnauthorizedException {
        return ok(views.html.netservice.CreateNetServiceCredential.render(asScala(netServiceManager.getAllNetServices()), createNetServiceCredentialsDtoForm));
    }

    public Result createNetServiceCredential() throws UnauthorizedException {
        Form<CreateNetServiceCredentialsDto> boundForm = createNetServiceCredentialsDtoForm.bindFromRequest();

        if(boundForm.hasErrors()){
            return badRequest(views.html.netservice.CreateNetServiceCredential.render(asScala(netServiceManager.getAllNetServices()),boundForm));
        }

        netServiceManager.createNetUserCredential(boundForm.get().getNetServiceId(), boundForm.get().getUsername(), boundForm.get().getPassword());


        return redirect(routes.NetServiceController.showUserNetServiceCredentials());
    }

    public Result deleteNetServiceCredential(){
        Form<DeleteNetServiceCredentialsDto> boundForm = deleteNetServiceCredentialsDtoForm.bindFromRequest();

        if(boundForm.hasErrors()){
            return redirect(routes.NetServiceController.showUserNetServiceCredentials());
        }

        netServiceManager.deleteNetServiceCredential(boundForm.get().getNetServiceCredentialId());

        return redirect(routes.NetServiceController.showUserNetServiceCredentials());


    }

    public Result decryptNetServiceCredential(Long credentialId) throws UnauthorizedException {
        NetService service = netServiceManager.getCredentialNetService(credentialId);
        PlaintextCredential credential = netServiceManager.decryptCredential(credentialId);

        ServiceCredentialDto dto = new ServiceCredentialDto(
            credential.getUsername(), credential.getPassword(),
            service.getUrl(), Collections.emptyList()
        );

        return ok(Json.toJson(dto));
    }
}
