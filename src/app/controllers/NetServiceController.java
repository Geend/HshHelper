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

import javax.inject.Inject;
import javax.inject.Singleton;
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
    private final Form<EditNetserviceDto> editNetserviceDtoForm;
    private final Form<AddNetServiceParameterDto> addNetServiceParameterDtoForm;
    private final Form<RemoveNetServiceParameterDto> removeNetServiceParameterDtoForm;
    private final Form<CreateNetServiceCredentialsDto> createNetServiceCredentialsDtoForm;
    private final Form<DeleteNetServiceCredentialsDto> deleteNetServiceCredentialsDtoForm;

    @Inject
    public NetServiceController(NetServiceManager netServiceManager, FormFactory formFactory){
        this.netServiceManager = netServiceManager;
        this.createNetServiceDtoForm = formFactory.form(CreateNetServiceDto.class);
        this.deleteNetServiceDtoForm = formFactory.form(DeleteNetServiceDto.class);
        this.editNetserviceDtoForm = formFactory.form(EditNetserviceDto.class);
        this.addNetServiceParameterDtoForm = formFactory.form(AddNetServiceParameterDto.class);
        this.removeNetServiceParameterDtoForm = formFactory.form(RemoveNetServiceParameterDto.class);
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
        NetService netService = netServiceManager.createNetService(dto.getName(), dto.getUrl());


        return redirect(routes.NetServiceController.showEditNetService(netService.getNetServiceId()));
    }

    public Result showEditNetService(Long netServiceId) throws UnauthorizedException {
        Optional<NetService> netServiceOpt = netServiceManager.getNetService(netServiceId);

        if(!netServiceOpt.isPresent())
            return redirect(routes.NetServiceController.showAllNetServices());

        NetService netService = netServiceOpt.get();

        AddNetServiceParameterDto addNetServiceParameterDto = new AddNetServiceParameterDto();
        addNetServiceParameterDto.setNetServiceId(netServiceId);

        EditNetserviceDto editNetserviceDto = new EditNetserviceDto(netServiceId,
                netService.getName(), netService.getUrl()
        );

        return ok(views.html.netservice.EditNetService.render(netService, addNetServiceParameterDtoForm.fill(addNetServiceParameterDto), editNetserviceDtoForm.fill(editNetserviceDto)));
    }


    public Result editNetService() throws UnauthorizedException, InvalidArgumentException {
        Form<EditNetserviceDto> boundForm = editNetserviceDtoForm.bindFromRequest();

        if(boundForm.hasErrors()){
            return showEditNetService(boundForm.get().getNetServiceId());
        }

        netServiceManager.editNetService(boundForm.get().getNetServiceId(),boundForm.get().getName(), boundForm.get().getUrl());

        return showEditNetService(boundForm.get().getNetServiceId());
    }

    public Result addNetServiceParameter() throws UnauthorizedException, InvalidArgumentException {
        Form<AddNetServiceParameterDto> boundForm = addNetServiceParameterDtoForm.bindFromRequest();

        if(boundForm.hasErrors()) {
            throw new InvalidArgumentException();
        }

        netServiceManager.addNetServiceParameter(boundForm.get().getNetServiceId(), boundForm.get().getName(), boundForm.get().getDefaultValue());
        return redirect(routes.NetServiceController.showEditNetService(boundForm.get().getNetServiceId()));
    }

    public Result removeNetServiceParameter() throws UnauthorizedException, InvalidArgumentException {
        Form<RemoveNetServiceParameterDto> boundForm = removeNetServiceParameterDtoForm.bindFromRequest();

        if(boundForm.hasErrors()) {
            throw new InvalidArgumentException();
        }

        netServiceManager.removeNetServiceParameter(boundForm.get().getNetServiceId(), boundForm.get().getNetServiceParameterId());
        return redirect(routes.NetServiceController.showEditNetService(boundForm.get().getNetServiceId()));
    }

    public Result showUserNetServiceCredentials(){

        List<NetServiceCredential> credentials = netServiceManager.getUserNetServiceCredentials();
        return ok(views.html.netservice.NetServiceCredentials.render(asScala(credentials)));
    }
    public Result showCreateNetServiceCredentialForm() throws UnauthorizedException {
        return ok(views.html.netservice.CreateNetServiceCredential.render(asScala(netServiceManager.getAllNetServices()), createNetServiceCredentialsDtoForm));
    }

    public Result createNetServiceCredential() throws UnauthorizedException, InvalidArgumentException {
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

    public Result decryptNetServiceCredential(Long credentialId) throws UnauthorizedException, InvalidArgumentException {
        PlaintextCredential credential = netServiceManager.decryptCredential(credentialId);
        return ok(Json.toJson(credential));
    }
}
