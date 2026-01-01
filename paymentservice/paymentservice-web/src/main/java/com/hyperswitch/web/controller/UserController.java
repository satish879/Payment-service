package com.hyperswitch.web.controller;

import com.hyperswitch.common.dto.*;
import com.hyperswitch.core.users.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for user management operations (v1 API)
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "User Management", description = "User management operations")
public class UserController {
    
    private static final Logger log = LoggerFactory.getLogger(UserController.class);
    
    private UserService userService;
    
    // Default constructor to allow bean creation even if dependencies are missing
    public UserController() {
        log.warn("UserController created without dependencies - services will be null");
    }
    
    @Autowired(required = false)
    public void setUserService(UserService userService) {
        this.userService = userService;
    }
    
    @PostConstruct
    public void init() {
        log.info("=== UserController BEAN CREATED ===");
        log.info("UserService available: {}", userService != null);
        if (userService == null) {
            log.warn("UserService is not available - user endpoints will not function properly");
        }
    }
    
    private <T> Mono<ResponseEntity<T>> checkServiceAvailable() {
        if (userService == null) {
            return Mono.just(ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .header("X-Error", "UserService not available")
                .body(null));
        }
        return null;
    }
    
    // Helper method to wrap service calls with null check
    private <T> Mono<ResponseEntity<T>> callService(Mono<com.hyperswitch.common.types.Result<T, com.hyperswitch.common.errors.PaymentError>> serviceCall) {
        Mono<ResponseEntity<T>> serviceCheck = checkServiceAvailable();
        if (serviceCheck != null) {
            return serviceCheck;
        }
        return serviceCall.map(result -> {
            if (result.isOk()) {
                return ResponseEntity.ok(result.unwrap());
            } else {
                throw new PaymentException(result.unwrapErr());
            }
        });
    }
    
    /**
     * Get user details
     * GET /api/user
     */
    @GetMapping
    @Operation(
        summary = "Get user details",
        description = "Retrieves the current user's details"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User details retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public Mono<ResponseEntity<UserResponse>> getUserDetails(
            @RequestHeader("user_id") String userId) {
        return callService(userService.getUserDetails(userId));
    }
    
    /**
     * User sign in
     * POST /api/user/signin
     */
    @PostMapping("/signin")
    @Operation(
        summary = "User sign in",
        description = "Signs in a user with email and password"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User signed in successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public Mono<ResponseEntity<AuthorizeResponse>> signIn(@RequestBody SignInRequest request) {
        return callService(userService.signIn(request));
    }
    
    /**
     * User sign in (v2)
     * POST /api/user/v2/signin
     */
    @PostMapping("/v2/signin")
    @Operation(
        summary = "User sign in (v2)",
        description = "Signs in a user with email and password (v2 API)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User signed in successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        ),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    public Mono<ResponseEntity<AuthorizeResponse>> signInV2(@RequestBody SignInRequest request) {
        return signIn(request);
    }
    
    /**
     * User sign up
     * POST /api/user/signup
     */
    @PostMapping("/signup")
    @Operation(
        summary = "User sign up",
        description = "Creates a new user account"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User signed up successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "User already exists")
    })
    public Mono<ResponseEntity<AuthorizeResponse>> signUp(@RequestBody SignUpRequest request) {
        return callService(userService.signUp(request));
    }
    
    /**
     * User sign up with merchant ID
     * POST /api/user/signup_with_merchant_id
     */
    @PostMapping("/signup_with_merchant_id")
    @Operation(
        summary = "User sign up with merchant ID",
        description = "Creates a new user account with merchant ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User signed up successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "User already exists")
    })
    public Mono<ResponseEntity<AuthorizeResponse>> signUpWithMerchantId(
            @RequestBody SignUpWithMerchantIdRequest request) {
        return callService(userService.signUpWithMerchantId(request));
    }
    
    /**
     * User sign out
     * POST /api/user/signout
     */
    @PostMapping("/signout")
    @Operation(
        summary = "User sign out",
        description = "Signs out the current user"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User signed out successfully"
        )
    })
    public Mono<ResponseEntity<Void>> signOut(@RequestHeader("user_id") String userId) {
        return userService.signOut(userId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Change password
     * POST /api/user/change_password
     */
    @PostMapping("/change_password")
    @Operation(
        summary = "Change password",
        description = "Changes the user's password"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Password changed successfully"
        ),
        @ApiResponse(responseCode = "400", description = "Invalid current password")
    })
    public Mono<ResponseEntity<Void>> changePassword(
            @RequestHeader("user_id") String userId,
            @RequestBody ChangePasswordRequest request) {
        return userService.changePassword(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Rotate password
     * POST /api/user/rotate_password
     */
    @PostMapping("/rotate_password")
    @Operation(
        summary = "Rotate password",
        description = "Rotates the user's password (admin operation)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Password rotated successfully"
        ),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public Mono<ResponseEntity<Void>> rotatePassword(
            @RequestHeader("user_id") String userId,
            @RequestBody RotatePasswordRequest request) {
        return userService.rotatePassword(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Forgot password
     * POST /api/user/forgot_password
     */
    @PostMapping("/forgot_password")
    @Operation(
        summary = "Forgot password",
        description = "Initiates password reset process"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Password reset email sent",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        )
    })
    public Mono<ResponseEntity<AuthorizeResponse>> forgotPassword(
            @RequestBody ForgotPasswordRequest request) {
        return userService.forgotPassword(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Reset password
     * POST /api/user/reset_password
     */
    @PostMapping("/reset_password")
    @Operation(
        summary = "Reset password",
        description = "Resets the user's password using a reset token"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Password reset successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid token")
    })
    public Mono<ResponseEntity<AuthorizeResponse>> resetPassword(
            @RequestBody ResetPasswordRequest request) {
        return userService.resetPassword(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Update user account
     * POST /api/user/update
     */
    @PostMapping("/update")
    @Operation(
        summary = "Update user account",
        description = "Updates the user's account details"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User updated successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public Mono<ResponseEntity<UserResponse>> updateUser(
            @RequestHeader("user_id") String userId,
            @RequestBody UpdateUserRequest request) {
        return userService.updateUser(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get user from email
     * POST /api/user/from_email
     */
    @PostMapping("/from_email")
    @Operation(
        summary = "Get user from email",
        description = "Retrieves user details by email address"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User retrieved successfully",
            content = @Content(schema = @Schema(implementation = UserResponse.class))
        ),
        @ApiResponse(responseCode = "404", description = "User not found")
    })
    public Mono<ResponseEntity<UserResponse>> getUserFromEmail(
            @RequestBody ForgotPasswordRequest request) {
        return userService.getUserFromEmail(request.getEmail())
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List organizations
     * GET /api/user/list/org
     */
    @GetMapping("/list/org")
    @Operation(
        summary = "List organizations",
        description = "Lists all organizations for the current user"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Organizations retrieved successfully"
        )
    })
    public Mono<ResponseEntity<java.util.List<OrganizationResponse>>> listOrganizations(
            @RequestHeader("user_id") String userId) {
        return userService.listOrganizations(userId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List merchants
     * GET /api/user/list/merchant
     */
    @GetMapping("/list/merchant")
    @Operation(
        summary = "List merchants",
        description = "Lists all merchants for the current user in organization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchants retrieved successfully"
        )
    })
    public Mono<ResponseEntity<java.util.List<MerchantAccountResponse>>> listMerchants(
            @RequestHeader("user_id") String userId,
            @RequestParam(required = false) String org_id) {
        return userService.listMerchants(userId, org_id)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List profiles
     * GET /api/user/list/profile
     */
    @GetMapping("/list/profile")
    @Operation(
        summary = "List profiles",
        description = "Lists all profiles for the current user in organization and merchant"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profiles retrieved successfully"
        )
    })
    public Mono<ResponseEntity<java.util.List<ProfileResponse>>> listProfiles(
            @RequestHeader("user_id") String userId,
            @RequestParam(required = false) String org_id,
            @RequestParam(required = false) String merchant_id) {
        return userService.listProfiles(userId, org_id, merchant_id)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Switch organization
     * POST /api/user/switch/org
     */
    @PostMapping("/switch/org")
    @Operation(
        summary = "Switch organization",
        description = "Switches the user's current organization context"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Organization switched successfully"
        )
    })
    public Mono<ResponseEntity<Void>> switchOrganization(
            @RequestHeader("user_id") String userId,
            @RequestBody SwitchOrganizationRequest request) {
        return userService.switchOrganization(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Switch merchant
     * POST /api/user/switch/merchant
     */
    @PostMapping("/switch/merchant")
    @Operation(
        summary = "Switch merchant",
        description = "Switches the user's current merchant context"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant switched successfully"
        )
    })
    public Mono<ResponseEntity<Void>> switchMerchant(
            @RequestHeader("user_id") String userId,
            @RequestBody SwitchMerchantRequest request) {
        return userService.switchMerchant(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Switch profile
     * POST /api/user/switch/profile
     */
    @PostMapping("/switch/profile")
    @Operation(
        summary = "Switch profile",
        description = "Switches the user's current profile context"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Profile switched successfully"
        )
    })
    public Mono<ResponseEntity<Void>> switchProfile(
            @RequestHeader("user_id") String userId,
            @RequestBody SwitchProfileRequest request) {
        return userService.switchProfile(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get dashboard metadata
     * GET /api/user/data
     */
    @GetMapping("/data")
    @Operation(
        summary = "Get dashboard metadata",
        description = "Retrieves the user's dashboard metadata"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dashboard metadata retrieved successfully",
            content = @Content(schema = @Schema(implementation = DashboardMetadataResponse.class))
        )
    })
    public Mono<ResponseEntity<DashboardMetadataResponse>> getDashboardMetadata(
            @RequestHeader("user_id") String userId) {
        return userService.getDashboardMetadata(userId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Set dashboard metadata
     * POST /api/user/data
     */
    @PostMapping("/data")
    @Operation(
        summary = "Set dashboard metadata",
        description = "Sets the user's dashboard metadata"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Dashboard metadata set successfully",
            content = @Content(schema = @Schema(implementation = DashboardMetadataResponse.class))
        )
    })
    public Mono<ResponseEntity<DashboardMetadataResponse>> setDashboardMetadata(
            @RequestHeader("user_id") String userId,
            @RequestBody DashboardMetadataRequest request) {
        return userService.setDashboardMetadata(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Create organization
     * POST /api/user/create_org
     */
    @PostMapping("/create_org")
    @Operation(
        summary = "Create organization",
        description = "Creates a new organization"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Organization created successfully",
            content = @Content(schema = @Schema(implementation = OrganizationResponse.class))
        )
    })
    public Mono<ResponseEntity<OrganizationResponse>> createOrganization(
            @RequestHeader("user_id") String userId,
            @RequestBody OrganizationRequest request) {
        return userService.createOrganization(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Create merchant account
     * POST /api/user/create_merchant
     */
    @PostMapping("/create_merchant")
    @Operation(
        summary = "Create merchant account",
        description = "Creates a new merchant account"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Merchant account created successfully",
            content = @Content(schema = @Schema(implementation = MerchantAccountResponse.class))
        )
    })
    public Mono<ResponseEntity<MerchantAccountResponse>> createMerchant(
            @RequestHeader("user_id") String userId,
            @RequestBody MerchantAccountCreateRequest request) {
        return userService.createMerchant(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Check 2FA status
     * GET /api/user/2fa
     */
    @GetMapping("/2fa")
    @Operation(
        summary = "Check 2FA status",
        description = "Checks the two-factor authentication status for the user"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "2FA status retrieved successfully",
            content = @Content(schema = @Schema(implementation = TwoFactorAuthStatusResponse.class))
        )
    })
    public Mono<ResponseEntity<TwoFactorAuthStatusResponse>> checkTwoFactorAuthStatus(
            @RequestHeader("user_id") String userId) {
        return userService.checkTwoFactorAuthStatus(userId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Check 2FA status with attempts
     * GET /api/user/2fa/v2
     */
    @GetMapping("/2fa/v2")
    @Operation(
        summary = "Check 2FA status with attempts",
        description = "Checks the two-factor authentication status with attempt count"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "2FA status retrieved successfully",
            content = @Content(schema = @Schema(implementation = TwoFactorAuthStatusResponse.class))
        )
    })
    public Mono<ResponseEntity<TwoFactorAuthStatusResponse>> checkTwoFactorAuthStatusWithAttempts(
            @RequestHeader("user_id") String userId) {
        return userService.checkTwoFactorAuthStatusWithAttempts(userId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Begin TOTP setup
     * GET /api/user/2fa/totp/begin
     */
    @GetMapping("/2fa/totp/begin")
    @Operation(
        summary = "Begin TOTP setup",
        description = "Begins the TOTP (Time-based One-Time Password) setup process"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "TOTP setup begun successfully",
            content = @Content(schema = @Schema(implementation = BeginTotpResponse.class))
        )
    })
    public Mono<ResponseEntity<BeginTotpResponse>> beginTotp(
            @RequestHeader("user_id") String userId) {
        return userService.beginTotp(userId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Reset TOTP
     * GET /api/user/2fa/totp/reset
     */
    @GetMapping("/2fa/totp/reset")
    @Operation(
        summary = "Reset TOTP",
        description = "Resets the TOTP configuration for the user"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "TOTP reset successfully",
            content = @Content(schema = @Schema(implementation = BeginTotpResponse.class))
        )
    })
    public Mono<ResponseEntity<BeginTotpResponse>> resetTotp(
            @RequestHeader("user_id") String userId) {
        return userService.resetTotp(userId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Verify TOTP
     * POST /api/user/2fa/totp/verify
     */
    @PostMapping("/2fa/totp/verify")
    @Operation(
        summary = "Verify TOTP",
        description = "Verifies a TOTP code"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "TOTP verified successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid TOTP code")
    })
    public Mono<ResponseEntity<AuthorizeResponse>> verifyTotp(
            @RequestHeader("user_id") String userId,
            @RequestBody VerifyTotpRequest request) {
        return userService.verifyTotp(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Update TOTP
     * PUT /api/user/2fa/totp/verify
     */
    @PutMapping("/2fa/totp/verify")
    @Operation(
        summary = "Update TOTP",
        description = "Updates the TOTP configuration"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "TOTP updated successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        )
    })
    public Mono<ResponseEntity<AuthorizeResponse>> updateTotp(
            @RequestHeader("user_id") String userId,
            @RequestBody VerifyTotpRequest request) {
        return userService.updateTotp(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Verify recovery code
     * POST /api/user/2fa/recovery_code/verify
     */
    @PostMapping("/2fa/recovery_code/verify")
    @Operation(
        summary = "Verify recovery code",
        description = "Verifies a recovery code for 2FA"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Recovery code verified successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid recovery code")
    })
    public Mono<ResponseEntity<AuthorizeResponse>> verifyRecoveryCode(
            @RequestHeader("user_id") String userId,
            @RequestBody VerifyRecoveryCodeRequest request) {
        return userService.verifyRecoveryCode(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Generate recovery codes
     * GET /api/user/2fa/recovery_code/generate
     */
    @GetMapping("/2fa/recovery_code/generate")
    @Operation(
        summary = "Generate recovery codes",
        description = "Generates new recovery codes for 2FA"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Recovery codes generated successfully",
            content = @Content(schema = @Schema(implementation = RecoveryCodesResponse.class))
        )
    })
    public Mono<ResponseEntity<RecoveryCodesResponse>> generateRecoveryCodes(
            @RequestHeader("user_id") String userId) {
        return userService.generateRecoveryCodes(userId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Terminate 2FA
     * GET /api/user/2fa/terminate
     */
    @GetMapping("/2fa/terminate")
    @Operation(
        summary = "Terminate 2FA",
        description = "Terminates two-factor authentication for the user"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "2FA terminated successfully"
        )
    })
    public Mono<ResponseEntity<Void>> terminateTwoFactorAuth(
            @RequestHeader("user_id") String userId,
            @RequestParam(required = false, defaultValue = "false") Boolean skip_two_factor_auth) {
        return userService.terminateTwoFactorAuth(userId, skip_two_factor_auth)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Verify email
     * POST /api/user/verify_email
     */
    @PostMapping("/verify_email")
    @Operation(
        summary = "Verify email",
        description = "Verifies the user's email address using a verification token"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Email verified successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        ),
        @ApiResponse(responseCode = "400", description = "Invalid token")
    })
    public Mono<ResponseEntity<AuthorizeResponse>> verifyEmail(
            @RequestBody VerifyEmailRequest request) {
        return userService.verifyEmail(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Verify email (v2)
     * POST /api/user/v2/verify_email
     */
    @PostMapping("/v2/verify_email")
    @Operation(
        summary = "Verify email (v2)",
        description = "Verifies the user's email address using a verification token (v2 API)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Email verified successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        )
    })
    public Mono<ResponseEntity<AuthorizeResponse>> verifyEmailV2(
            @RequestBody VerifyEmailRequest request) {
        return verifyEmail(request);
    }
    
    /**
     * Request email verification
     * POST /api/user/verify_email_request
     */
    @PostMapping("/verify_email_request")
    @Operation(
        summary = "Request email verification",
        description = "Requests an email verification token to be sent"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Verification email sent",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        )
    })
    public Mono<ResponseEntity<AuthorizeResponse>> requestEmailVerification(
            @RequestBody ForgotPasswordRequest request) {
        return userService.requestEmailVerification(request.getEmail())
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Internal user signup
     * POST /api/user/internal_signup
     */
    @PostMapping("/internal_signup")
    @Operation(
        summary = "Internal user signup",
        description = "Creates a new internal user account"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Internal user created successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        )
    })
    public Mono<ResponseEntity<AuthorizeResponse>> internalSignup(
            @RequestBody InternalSignupRequest request) {
        return userService.internalSignup(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Create tenant user
     * POST /api/user/tenant_signup
     */
    @PostMapping("/tenant_signup")
    @Operation(
        summary = "Create tenant user",
        description = "Creates a new tenant user account"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Tenant user created successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        )
    })
    public Mono<ResponseEntity<AuthorizeResponse>> createTenantUser(
            @RequestBody InternalSignupRequest request) {
        return userService.createTenantUser(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Connect account
     * POST /api/user/connect_account
     */
    @PostMapping("/connect_account")
    @Operation(
        summary = "Connect account",
        description = "Connects an account by sending a connection email"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Connection email sent",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        )
    })
    public Mono<ResponseEntity<AuthorizeResponse>> connectAccount(
            @RequestBody ConnectAccountRequest request) {
        return userService.connectAccount(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Invite multiple users
     * POST /api/user/user/invite_multiple
     */
    @PostMapping("/user/invite_multiple")
    @Operation(
        summary = "Invite multiple users",
        description = "Invites multiple users to the organization/merchant/profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users invited successfully"
        )
    })
    public Mono<ResponseEntity<java.util.List<InviteMultipleUserResponse>>> inviteMultipleUsers(
            @RequestHeader("user_id") String userId,
            @RequestBody java.util.List<InviteUserRequest> requests) {
        return userService.inviteMultipleUsers(userId, requests)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Resend invitation
     * POST /api/user/user/resend_invite
     */
    @PostMapping("/user/resend_invite")
    @Operation(
        summary = "Resend invitation",
        description = "Resends an invitation email to a user"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Invitation resent successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        )
    })
    public Mono<ResponseEntity<AuthorizeResponse>> resendInvite(
            @RequestHeader("user_id") String userId,
            @RequestBody ReInviteUserRequest request) {
        return userService.resendInvite(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Accept invitation from email
     * POST /api/user/accept_invite_from_email
     */
    @PostMapping("/accept_invite_from_email")
    @Operation(
        summary = "Accept invitation from email",
        description = "Accepts an invitation using a token from email"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Invitation accepted successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        )
    })
    public Mono<ResponseEntity<AuthorizeResponse>> acceptInviteFromEmail(
            @RequestBody AcceptInviteFromEmailRequest request) {
        return userService.acceptInviteFromEmail(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Terminate accept invite
     * POST /api/user/terminate_accept_invite
     */
    @PostMapping("/terminate_accept_invite")
    @Operation(
        summary = "Terminate accept invite",
        description = "Terminates the invitation acceptance process"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Invitation acceptance terminated successfully"
        )
    })
    public Mono<ResponseEntity<Void>> terminateAcceptInvite(
            @RequestHeader("user_id") String userId,
            @RequestBody AcceptInviteFromEmailRequest request) {
        return userService.terminateAcceptInvite(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List user roles details
     * POST /api/user/user
     */
    @PostMapping("/user")
    @Operation(
        summary = "List user roles details",
        description = "Lists role details for a user by email"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User roles details retrieved successfully"
        )
    })
    public Mono<ResponseEntity<java.util.List<UserRoleDetailsResponse>>> listUserRolesDetails(
            @RequestHeader("user_id") String userId,
            @RequestBody GetUserRoleDetailsRequest request) {
        return userService.listUserRolesDetails(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List user roles details (v2)
     * POST /api/user/user/v2
     */
    @PostMapping("/user/v2")
    @Operation(
        summary = "List user roles details (v2)",
        description = "Lists role details for a user by email (v2 API)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User roles details retrieved successfully"
        )
    })
    public Mono<ResponseEntity<java.util.List<UserRoleDetailsResponse>>> listUserRolesDetailsV2(
            @RequestHeader("user_id") String userId,
            @RequestBody GetUserRoleDetailsRequest request) {
        return listUserRolesDetails(userId, request);
    }
    
    /**
     * List users in lineage
     * GET /api/user/user/list
     */
    @GetMapping("/user/list")
    @Operation(
        summary = "List users in lineage",
        description = "Lists all users in the same lineage (org/merchant/profile)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully"
        )
    })
    public Mono<ResponseEntity<java.util.List<UserResponse>>> listUsersInLineage(
            @RequestHeader("user_id") String userId) {
        return userService.listUsersInLineage(userId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List users in lineage (v2)
     * GET /api/user/user/v2/list
     */
    @GetMapping("/user/v2/list")
    @Operation(
        summary = "List users in lineage (v2)",
        description = "Lists all users in the same lineage (v2 API)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Users retrieved successfully"
        )
    })
    public Mono<ResponseEntity<java.util.List<UserResponse>>> listUsersInLineageV2(
            @RequestHeader("user_id") String userId) {
        return listUsersInLineage(userId);
    }
    
    /**
     * Update user role
     * POST /api/user/user/update_role
     */
    @PostMapping("/user/update_role")
    @Operation(
        summary = "Update user role",
        description = "Updates the role of a user"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User role updated successfully",
            content = @Content(schema = @Schema(implementation = UserRoleDetailsResponse.class))
        )
    })
    public Mono<ResponseEntity<UserRoleDetailsResponse>> updateUserRole(
            @RequestHeader("user_id") String userId,
            @RequestParam("target_user_id") String targetUserId,
            @RequestBody UpdateUserRoleRequest request) {
        return userService.updateUserRole(userId, targetUserId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Delete user role
     * DELETE /api/user/user/delete
     */
    @DeleteMapping("/user/delete")
    @Operation(
        summary = "Delete user role",
        description = "Deletes a user's role"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User role deleted successfully"
        )
    })
    public Mono<ResponseEntity<Void>> deleteUserRole(
            @RequestHeader("user_id") String userId,
            @RequestParam("target_user_id") String targetUserId) {
        return userService.deleteUserRole(userId, targetUserId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get role from token
     * GET /api/user/role
     */
    @GetMapping("/role")
    @Operation(
        summary = "Get role from token",
        description = "Gets the role information from the current user's token"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role retrieved successfully",
            content = @Content(schema = @Schema(implementation = RoleResponse.class))
        )
    })
    public Mono<ResponseEntity<RoleResponse>> getRoleFromToken(
            @RequestHeader("user_id") String userId) {
        return userService.getRoleFromToken(userId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Create role
     * POST /api/user/role
     */
    @PostMapping("/role")
    @Operation(
        summary = "Create role",
        description = "Creates a new role"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role created successfully",
            content = @Content(schema = @Schema(implementation = RoleResponse.class))
        )
    })
    public Mono<ResponseEntity<RoleResponse>> createRole(
            @RequestHeader("user_id") String userId,
            @RequestBody CreateRoleRequest request) {
        return userService.createRole(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Create role (v2)
     * POST /api/user/role/v2
     */
    @PostMapping("/role/v2")
    @Operation(
        summary = "Create role (v2)",
        description = "Creates a new role (v2 API)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role created successfully",
            content = @Content(schema = @Schema(implementation = RoleResponse.class))
        )
    })
    public Mono<ResponseEntity<RoleResponse>> createRoleV2(
            @RequestHeader("user_id") String userId,
            @RequestBody CreateRoleRequest request) {
        return createRole(userId, request);
    }
    
    /**
     * Get groups and resources for role
     * GET /api/user/role/v2
     */
    @GetMapping("/role/v2")
    @Operation(
        summary = "Get groups and resources for role",
        description = "Gets groups and resources for the current user's role"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Groups and resources retrieved successfully",
            content = @Content(schema = @Schema(implementation = RoleResponse.class))
        )
    })
    public Mono<ResponseEntity<RoleResponse>> getGroupsAndResourcesForRole(
            @RequestHeader("user_id") String userId) {
        return getRoleFromToken(userId); // Similar to get role from token
    }
    
    /**
     * Get parent groups info for role
     * GET /api/user/role/v3
     */
    @GetMapping("/role/v3")
    @Operation(
        summary = "Get parent groups info for role",
        description = "Gets parent groups information for the current user's role"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Parent groups info retrieved successfully",
            content = @Content(schema = @Schema(implementation = RoleResponse.class))
        )
    })
    public Mono<ResponseEntity<RoleResponse>> getParentGroupsInfoForRole(
            @RequestHeader("user_id") String userId) {
        return getRoleFromToken(userId); // Similar to get role from token
    }
    
    /**
     * List roles with info
     * GET /api/user/role/v2/list
     */
    @GetMapping("/role/v2/list")
    @Operation(
        summary = "List roles with info",
        description = "Lists all roles with their information"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Roles retrieved successfully"
        )
    })
    public Mono<ResponseEntity<java.util.List<RoleResponse>>> listRolesWithInfo(
            @RequestHeader("user_id") String userId) {
        return userService.listRoles(userId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List roles with info
     * GET /api/user/role/list
     */
    @GetMapping("/role/list")
    @Operation(
        summary = "List roles with info",
        description = "Lists all roles with their information"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Roles retrieved successfully"
        )
    })
    public Mono<ResponseEntity<java.util.List<RoleResponse>>> listRoles(
            @RequestHeader("user_id") String userId) {
        return listRolesWithInfo(userId);
    }
    
    /**
     * List invitable roles
     * GET /api/user/role/list/invite
     */
    @GetMapping("/role/list/invite")
    @Operation(
        summary = "List invitable roles",
        description = "Lists roles that can be used for invitations"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Invitable roles retrieved successfully"
        )
    })
    public Mono<ResponseEntity<java.util.List<RoleResponse>>> listInvitableRoles(
            @RequestHeader("user_id") String userId) {
        return listRoles(userId); // Same as list roles
    }
    
    /**
     * List updatable roles
     * GET /api/user/role/list/update
     */
    @GetMapping("/role/list/update")
    @Operation(
        summary = "List updatable roles",
        description = "Lists roles that can be updated"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Updatable roles retrieved successfully"
        )
    })
    public Mono<ResponseEntity<java.util.List<RoleResponse>>> listUpdatableRoles(
            @RequestHeader("user_id") String userId) {
        return listRoles(userId); // Same as list roles
    }
    
    /**
     * Get role
     * GET /api/user/role/{role_id}
     */
    @GetMapping("/role/{role_id}")
    @Operation(
        summary = "Get role",
        description = "Retrieves a role by its ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role retrieved successfully",
            content = @Content(schema = @Schema(implementation = RoleResponse.class))
        )
    })
    public Mono<ResponseEntity<RoleResponse>> getRole(
            @RequestHeader("user_id") String userId,
            @PathVariable("role_id") String roleId) {
        return userService.getRole(userId, roleId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Update role
     * PUT /api/user/role/{role_id}
     */
    @PutMapping("/role/{role_id}")
    @Operation(
        summary = "Update role",
        description = "Updates a role"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Role updated successfully",
            content = @Content(schema = @Schema(implementation = RoleResponse.class))
        )
    })
    public Mono<ResponseEntity<RoleResponse>> updateRole(
            @RequestHeader("user_id") String userId,
            @PathVariable("role_id") String roleId,
            @RequestBody CreateRoleRequest request) {
        return userService.updateRole(userId, roleId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get parent info for role
     * GET /api/user/role/{role_id}/v2
     */
    @GetMapping("/role/{role_id}/v2")
    @Operation(
        summary = "Get parent info for role",
        description = "Gets parent groups information for a role"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Parent info retrieved successfully",
            content = @Content(schema = @Schema(implementation = RoleResponse.class))
        )
    })
    public Mono<ResponseEntity<RoleResponse>> getParentInfoForRole(
            @RequestHeader("user_id") String userId,
            @PathVariable("role_id") String roleId) {
        return getRole(userId, roleId); // Similar to get role
    }
    
    /**
     * Get theme using lineage (admin)
     * GET /api/user/admin/theme
     */
    @GetMapping("/admin/theme")
    @Operation(
        summary = "Get theme using lineage",
        description = "Gets theme using lineage context (admin)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Theme retrieved successfully",
            content = @Content(schema = @Schema(implementation = ThemeResponse.class))
        )
    })
    public Mono<ResponseEntity<ThemeResponse>> getThemeUsingLineage(
            @RequestHeader("user_id") String userId,
            @RequestParam(required = false) String entity_type) {
        return userService.getThemeUsingLineage(userId, entity_type != null ? entity_type : "ORGANIZATION")
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Create theme (admin)
     * POST /api/user/admin/theme
     */
    @PostMapping("/admin/theme")
    @Operation(
        summary = "Create theme",
        description = "Creates a new theme (admin)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Theme created successfully",
            content = @Content(schema = @Schema(implementation = ThemeResponse.class))
        )
    })
    public Mono<ResponseEntity<ThemeResponse>> createTheme(
            @RequestHeader("user_id") String userId,
            @RequestBody CreateThemeRequest request) {
        return userService.createTheme(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get theme using theme ID (admin)
     * GET /api/user/admin/theme/{theme_id}
     */
    @GetMapping("/admin/theme/{theme_id}")
    @Operation(
        summary = "Get theme using theme ID",
        description = "Gets theme by theme ID (admin)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Theme retrieved successfully",
            content = @Content(schema = @Schema(implementation = ThemeResponse.class))
        )
    })
    public Mono<ResponseEntity<ThemeResponse>> getThemeUsingThemeId(
            @RequestHeader("user_id") String userId,
            @PathVariable("theme_id") String themeId) {
        return userService.getThemeUsingThemeId(userId, themeId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Update theme (admin)
     * PUT /api/user/admin/theme/{theme_id}
     */
    @PutMapping("/admin/theme/{theme_id}")
    @Operation(
        summary = "Update theme",
        description = "Updates a theme (admin)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Theme updated successfully",
            content = @Content(schema = @Schema(implementation = ThemeResponse.class))
        )
    })
    public Mono<ResponseEntity<ThemeResponse>> updateTheme(
            @RequestHeader("user_id") String userId,
            @PathVariable("theme_id") String themeId,
            @RequestBody UpdateThemeRequest request) {
        return userService.updateTheme(userId, themeId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Upload file to theme storage (admin)
     * POST /api/user/admin/theme/{theme_id}
     */
    @PostMapping("/admin/theme/{theme_id}")
    @Operation(
        summary = "Upload file to theme storage",
        description = "Uploads a file to theme storage (admin)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "File uploaded successfully"
        )
    })
    public Mono<ResponseEntity<Void>> uploadFileToThemeStorage(
            @RequestHeader("user_id") String userId,
            @PathVariable("theme_id") String themeId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        // In production, handle file upload
        return Mono.just(ResponseEntity.ok().build());
    }
    
    /**
     * Delete theme (admin)
     * DELETE /api/user/admin/theme/{theme_id}
     */
    @DeleteMapping("/admin/theme/{theme_id}")
    @Operation(
        summary = "Delete theme",
        description = "Deletes a theme (admin)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Theme deleted successfully"
        )
    })
    public Mono<ResponseEntity<Void>> deleteTheme(
            @RequestHeader("user_id") String userId,
            @PathVariable("theme_id") String themeId) {
        return userService.deleteTheme(userId, themeId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Create user theme
     * POST /api/user/theme
     */
    @PostMapping("/theme")
    @Operation(
        summary = "Create user theme",
        description = "Creates a new user theme"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Theme created successfully",
            content = @Content(schema = @Schema(implementation = ThemeResponse.class))
        )
    })
    public Mono<ResponseEntity<ThemeResponse>> createUserTheme(
            @RequestHeader("user_id") String userId,
            @RequestBody CreateThemeRequest request) {
        return userService.createUserTheme(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get user theme using lineage
     * GET /api/user/theme
     */
    @GetMapping("/theme")
    @Operation(
        summary = "Get user theme using lineage",
        description = "Gets user theme using lineage context"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Theme retrieved successfully",
            content = @Content(schema = @Schema(implementation = ThemeResponse.class))
        )
    })
    public Mono<ResponseEntity<ThemeResponse>> getUserThemeUsingLineage(
            @RequestHeader("user_id") String userId,
            @RequestParam(required = false) String entity_type) {
        return userService.getUserThemeUsingLineage(userId, entity_type != null ? entity_type : "ORGANIZATION")
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List all themes in lineage
     * GET /api/user/theme/list
     */
    @GetMapping("/theme/list")
    @Operation(
        summary = "List all themes in lineage",
        description = "Lists all themes in the lineage context"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Themes retrieved successfully"
        )
    })
    public Mono<ResponseEntity<java.util.List<ThemeResponse>>> listAllThemesInLineage(
            @RequestHeader("user_id") String userId,
            @RequestParam(required = false) String entity_type) {
        return userService.listAllThemesInLineage(userId, entity_type != null ? entity_type : "ORGANIZATION")
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get user theme using theme ID
     * GET /api/user/theme/{theme_id}
     */
    @GetMapping("/theme/{theme_id}")
    @Operation(
        summary = "Get user theme using theme ID",
        description = "Gets user theme by theme ID"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Theme retrieved successfully",
            content = @Content(schema = @Schema(implementation = ThemeResponse.class))
        )
    })
    public Mono<ResponseEntity<ThemeResponse>> getUserThemeUsingThemeId(
            @RequestHeader("user_id") String userId,
            @PathVariable("theme_id") String themeId) {
        return userService.getUserThemeUsingThemeId(userId, themeId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Update user theme
     * PUT /api/user/theme/{theme_id}
     */
    @PutMapping("/theme/{theme_id}")
    @Operation(
        summary = "Update user theme",
        description = "Updates a user theme"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Theme updated successfully",
            content = @Content(schema = @Schema(implementation = ThemeResponse.class))
        )
    })
    public Mono<ResponseEntity<ThemeResponse>> updateUserTheme(
            @RequestHeader("user_id") String userId,
            @PathVariable("theme_id") String themeId,
            @RequestBody UpdateThemeRequest request) {
        return userService.updateUserTheme(userId, themeId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Upload file to user theme storage
     * POST /api/user/theme/{theme_id}
     */
    @PostMapping("/theme/{theme_id}")
    @Operation(
        summary = "Upload file to user theme storage",
        description = "Uploads a file to user theme storage"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "File uploaded successfully"
        )
    })
    public Mono<ResponseEntity<Void>> uploadFileToUserThemeStorage(
            @RequestHeader("user_id") String userId,
            @PathVariable("theme_id") String themeId,
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file) {
        // In production, handle file upload
        return Mono.just(ResponseEntity.ok().build());
    }
    
    /**
     * Delete user theme
     * DELETE /api/user/theme/{theme_id}
     */
    @DeleteMapping("/theme/{theme_id}")
    @Operation(
        summary = "Delete user theme",
        description = "Deletes a user theme"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Theme deleted successfully"
        )
    })
    public Mono<ResponseEntity<Void>> deleteUserTheme(
            @RequestHeader("user_id") String userId,
            @PathVariable("theme_id") String themeId) {
        return userService.deleteUserTheme(userId, themeId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Generate sample data
     * POST /api/user/sample_data
     */
    @PostMapping("/sample_data")
    @Operation(
        summary = "Generate sample data",
        description = "Generates sample payment data for testing"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Sample data generated successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        )
    })
    public Mono<ResponseEntity<AuthorizeResponse>> generateSampleData(
            @RequestHeader("user_id") String userId,
            @RequestBody SampleDataRequest request) {
        return userService.generateSampleData(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Delete sample data
     * DELETE /api/user/sample_data
     */
    @DeleteMapping("/sample_data")
    @Operation(
        summary = "Delete sample data",
        description = "Deletes sample payment data"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Sample data deleted successfully"
        )
    })
    public Mono<ResponseEntity<Void>> deleteSampleData(
            @RequestHeader("user_id") String userId,
            @RequestBody SampleDataRequest request) {
        return userService.deleteSampleData(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok().build();
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Clone connector
     * POST /api/user/clone_connector
     */
    @PostMapping("/clone_connector")
    @Operation(
        summary = "Clone connector",
        description = "Clones a connector configuration to another merchant/profile"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Connector cloned successfully",
            content = @Content(schema = @Schema(implementation = CloneConnectorResponse.class))
        )
    })
    public Mono<ResponseEntity<CloneConnectorResponse>> cloneConnector(
            @RequestHeader("user_id") String userId,
            @RequestBody CloneConnectorRequest request) {
        return userService.cloneConnector(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Create authentication method
     * POST /api/user/auth
     */
    @PostMapping("/auth")
    @Operation(
        summary = "Create authentication method",
        description = "Creates a new authentication method (SSO/OIDC)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authentication method created successfully",
            content = @Content(schema = @Schema(implementation = CreateUserAuthenticationMethodResponse.class))
        )
    })
    public Mono<ResponseEntity<CreateUserAuthenticationMethodResponse>> createUserAuthenticationMethod(
            @RequestBody CreateUserAuthenticationMethodRequest request) {
        return userService.createUserAuthenticationMethod(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Update authentication method
     * PUT /api/user/auth
     */
    @PutMapping("/auth")
    @Operation(
        summary = "Update authentication method",
        description = "Updates an existing authentication method"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authentication method updated successfully",
            content = @Content(schema = @Schema(implementation = CreateUserAuthenticationMethodResponse.class))
        )
    })
    public Mono<ResponseEntity<CreateUserAuthenticationMethodResponse>> updateUserAuthenticationMethod(
            @RequestBody UpdateUserAuthenticationMethodRequest request) {
        return userService.updateUserAuthenticationMethod(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List authentication methods
     * GET /api/user/auth/list
     */
    @GetMapping("/auth/list")
    @Operation(
        summary = "List authentication methods",
        description = "Lists all authentication methods"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Authentication methods retrieved successfully"
        )
    })
    public Mono<ResponseEntity<java.util.List<AuthenticationMethodResponse>>> listUserAuthenticationMethods(
            @RequestParam(required = false) String auth_id) {
        return userService.listUserAuthenticationMethods(auth_id != null ? auth_id : "")
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get SSO auth URL
     * GET /api/user/auth/url
     */
    @GetMapping("/auth/url")
    @Operation(
        summary = "Get SSO auth URL",
        description = "Gets the SSO authentication URL"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "SSO auth URL retrieved successfully",
            content = @Content(schema = @Schema(implementation = SsoAuthUrlResponse.class))
        )
    })
    public Mono<ResponseEntity<SsoAuthUrlResponse>> getSsoAuthUrl(
            @RequestParam("auth_id") String authId) {
        GetSsoAuthUrlRequest request = new GetSsoAuthUrlRequest();
        request.setAuthId(authId);
        return userService.getSsoAuthUrl(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * SSO sign in
     * POST /api/user/oidc
     */
    @PostMapping("/oidc")
    @Operation(
        summary = "SSO sign in",
        description = "Signs in using SSO/OIDC"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "SSO sign in successful",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        )
    })
    public Mono<ResponseEntity<AuthorizeResponse>> ssoSignIn(
            @RequestBody SsoSignInRequest request) {
        return userService.ssoSignIn(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Terminate auth select
     * POST /api/user/auth/select
     */
    @PostMapping("/auth/select")
    @Operation(
        summary = "Terminate auth select",
        description = "Terminates the authentication selection process"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Auth select terminated successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        )
    })
    public Mono<ResponseEntity<AuthorizeResponse>> terminateAuthSelect(
            @RequestHeader("user_id") String userId,
            @RequestBody AuthSelectRequest request) {
        return userService.terminateAuthSelect(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Transfer user key
     * POST /api/user/key/transfer
     */
    @PostMapping("/key/transfer")
    @Operation(
        summary = "Transfer user key",
        description = "Transfers user keys"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "User keys transferred successfully",
            content = @Content(schema = @Schema(implementation = UserKeyTransferResponse.class))
        )
    })
    public Mono<ResponseEntity<UserKeyTransferResponse>> transferUserKey(
            @RequestBody UserKeyTransferRequest request) {
        return userService.transferUserKey(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Create platform
     * POST /api/user/create_platform
     */
    @PostMapping("/create_platform")
    @Operation(
        summary = "Create platform",
        description = "Creates a new platform account"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Platform created successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        )
    })
    public Mono<ResponseEntity<AuthorizeResponse>> createPlatform(
            @RequestHeader("user_id") String userId,
            @RequestBody CreatePlatformRequest request) {
        return userService.createPlatform(userId, request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get permission info
     * GET /api/user/permission_info
     */
    @GetMapping("/permission_info")
    @Operation(
        summary = "Get permission info",
        description = "Gets authorization and permission information"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Permission info retrieved successfully",
            content = @Content(schema = @Schema(implementation = PermissionInfoResponse.class))
        )
    })
    public Mono<ResponseEntity<PermissionInfoResponse>> getPermissionInfo(
            @RequestHeader("user_id") String userId) {
        return userService.getPermissionInfo(userId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get module list
     * GET /api/user/module/list
     */
    @GetMapping("/module/list")
    @Operation(
        summary = "Get module list",
        description = "Gets role information and module list"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Module list retrieved successfully",
            content = @Content(schema = @Schema(implementation = ModuleListResponse.class))
        )
    })
    public Mono<ResponseEntity<ModuleListResponse>> getModuleList(
            @RequestHeader("user_id") String userId) {
        return userService.getModuleList(userId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Get parent list
     * GET /api/user/parent/list
     */
    @GetMapping("/parent/list")
    @Operation(
        summary = "Get parent list",
        description = "Gets parent group information"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Parent list retrieved successfully",
            content = @Content(schema = @Schema(implementation = ParentListResponse.class))
        )
    })
    public Mono<ResponseEntity<ParentListResponse>> getParentList(
            @RequestHeader("user_id") String userId) {
        return userService.getParentList(userId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * List invitations
     * GET /api/user/list/invitation
     */
    @GetMapping("/list/invitation")
    @Operation(
        summary = "List invitations",
        description = "Lists all invitations"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Invitations retrieved successfully",
            content = @Content(schema = @Schema(implementation = InvitationListResponse.class))
        )
    })
    public Mono<ResponseEntity<InvitationListResponse>> listInvitations(
            @RequestHeader("user_id") String userId) {
        return userService.listInvitations(userId)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Accept invitations
     * POST /api/user/user/invite/accept
     */
    @PostMapping("/user/invite/accept")
    @Operation(
        summary = "Accept invitations",
        description = "Accepts an invitation"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Invitation accepted successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        )
    })
    public Mono<ResponseEntity<AuthorizeResponse>> acceptInvitations(
            @RequestHeader("user_id") String userId,
            @RequestBody AcceptInviteFromEmailRequest request) {
        return userService.acceptInviteFromEmail(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Accept invitations pre-auth
     * POST /api/user/user/invite/accept/pre_auth
     */
    @PostMapping("/user/invite/accept/pre_auth")
    @Operation(
        summary = "Accept invitations pre-auth",
        description = "Accepts an invitation with pre-authentication"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Invitation accepted successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        )
    })
    public Mono<ResponseEntity<AuthorizeResponse>> acceptInvitationsPreAuth(
            @RequestBody AcceptInviteFromEmailRequest request) {
        // Pre-auth means no user_id header required
        return userService.acceptInviteFromEmail(request)
            .map(result -> {
                if (result.isOk()) {
                    return ResponseEntity.ok(result.unwrap());
                } else {
                    throw new PaymentException(result.unwrapErr());
                }
            });
    }
    
    /**
     * Accept invitations (v2)
     * POST /api/user/user/invite/accept/v2
     */
    @PostMapping("/user/invite/accept/v2")
    @Operation(
        summary = "Accept invitations (v2)",
        description = "Accepts an invitation (v2 API)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Invitation accepted successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        )
    })
    public Mono<ResponseEntity<AuthorizeResponse>> acceptInvitationsV2(
            @RequestHeader("user_id") String userId,
            @RequestBody AcceptInviteFromEmailRequest request) {
        return acceptInvitations(userId, request);
    }
    
    /**
     * Accept invitations pre-auth (v2)
     * POST /api/user/user/invite/accept/v2/pre_auth
     */
    @PostMapping("/user/invite/accept/v2/pre_auth")
    @Operation(
        summary = "Accept invitations pre-auth (v2)",
        description = "Accepts an invitation with pre-authentication (v2 API)"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Invitation accepted successfully",
            content = @Content(schema = @Schema(implementation = AuthorizeResponse.class))
        )
    })
    public Mono<ResponseEntity<AuthorizeResponse>> acceptInvitationsPreAuthV2(
            @RequestBody AcceptInviteFromEmailRequest request) {
        return acceptInvitationsPreAuth(request);
    }
}

