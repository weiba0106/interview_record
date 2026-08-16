package com.interviewrecord.sharing.api;

import com.interviewrecord.common.security.CurrentUser;
import com.interviewrecord.common.util.ResourceIds;
import com.interviewrecord.sharing.application.SharingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;

@RestController
@RequestMapping("/api/v1")
@ConditionalOnExpression("'${spring.datasource.url:}' != ''")
public class SharingController {
    private final CurrentUser currentUser;
    private final SharingService sharing;
    public SharingController(CurrentUser currentUser, SharingService sharing) { this.currentUser = currentUser; this.sharing = sharing; }

    @PostMapping("/positions/{positionId}/shares") @ResponseStatus(HttpStatus.CREATED)
    SharingDtos.CreatedShareResponse create(@PathVariable String positionId, @Valid @RequestBody SharingDtos.CreateShareRequest request) {
        return sharing.create(currentUser.require().id(), ResourceIds.parse(positionId), request);
    }
    @GetMapping("/positions/{positionId}/shares")
    List<SharingDtos.ShareLinkResponse> list(@PathVariable String positionId) {
        return sharing.list(currentUser.require().id(), ResourceIds.parse(positionId));
    }
    @DeleteMapping("/positions/{positionId}/shares/{shareId}") @ResponseStatus(HttpStatus.NO_CONTENT)
    void revoke(@PathVariable String positionId, @PathVariable String shareId) {
        sharing.revoke(currentUser.require().id(), ResourceIds.parse(positionId), ResourceIds.parse(shareId));
    }
    @GetMapping("/shares/{token}")
    ResponseEntity<SharingDtos.PublicShareResponse> publicView(@PathVariable String token, HttpServletRequest request) {
        return ResponseEntity.ok().header("X-Robots-Tag", "noindex, nofollow")
                .body(sharing.getPublic(token, request.getRemoteAddr()));
    }
}
