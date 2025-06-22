package com.PayMyBuddy.controller;

import com.PayMyBuddy.model.UserAccount;
import com.PayMyBuddy.service.ConnectionService;
import com.PayMyBuddy.service.TransactionService;
import com.PayMyBuddy.service.UserAccountService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.Collections;
import java.util.Optional;

@Controller
public class WebController {

    private final UserAccountService userAccountService;
    private final ConnectionService connectionService;
    private final TransactionService transactionService;

    public WebController(UserAccountService userAccountService,
                        ConnectionService connectionService,
                        TransactionService transactionService) {
        this.userAccountService = userAccountService;
        this.connectionService = connectionService;
        this.transactionService = transactionService;
    }

    @GetMapping("/")
    public String index() {
        // Si l'utilisateur est déjà authentifié, redirigez-le vers le dashboard
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.isAuthenticated() && !auth.getName().equals("anonymousUser")) {
            return "redirect:/dashboard";
        }
        // Sinon, affichez la page d'accueil
        return "index";
    }

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new UserAccount());
        return "register";
    }

    @PostMapping("/register")
    public String registerUser(@ModelAttribute("user") UserAccount user) {
        userAccountService.save(user);

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), user.getPassword(),
                Collections.emptyList()
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();

        // Vérifier si l'utilisateur est authentifié mais, n'est pas l'utilisateur anonyme
        if (auth == null || auth.getName().equals("anonymousUser")) {
            return "redirect:/login";
        }

        // Rechercher l'utilisateur par email et rediriger si non trouvé
        Optional<UserAccount> optionalUser = userAccountService.findByEmail(auth.getName());
        if (optionalUser.isEmpty()) {
            // L'email de l'utilisateur authentifié n'existe pas dans la base de données
            return "redirect:/login?error=usernotfound";
        }

        UserAccount user = optionalUser.get();
        model.addAttribute("user", user);
        model.addAttribute("connections", connectionService.findByOwnerId(user.getId()));
        model.addAttribute("transactions", transactionService.findByUser(user));

        return "dashboard";
    }

    @GetMapping("/connections")
    public String connectionsPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountService.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        model.addAttribute("user", user);
        model.addAttribute("connections", connectionService.findByOwnerId(user.getId()));

        return "connections";
    }

    @GetMapping("/transactions")
    public String transactionsPage(Model model) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountService.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        model.addAttribute("user", user);
        model.addAttribute("connections", connectionService.findByOwnerId(user.getId()));
        model.addAttribute("transactions", transactionService.findByUser(user));

        return "transactions";
    }

    @PostMapping("/connections/add")
    public String addConnection(@ModelAttribute("email") String friendEmail) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountService.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        // Vérifier si l'utilisateur essaie d'ajouter sa propre adresse e-mail
        if (user.getEmail().equals(friendEmail)) {
            return "redirect:/connections?error=self_connection";
        }

        try {
            UserAccount friend = userAccountService.findByEmail(friendEmail)
                    .orElseThrow(() -> new IllegalArgumentException("Contact non trouvé"));

            connectionService.createConnection(user.getId(), friend.getId());
            return "redirect:/connections?success";
        } catch (IllegalArgumentException e) {
            // Encoder le message d'erreur pour l'URL
            String errorMessage = java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/connections?error=" + errorMessage;
        }
    }

    @PostMapping("/transactions/send")
    public String sendMoney(@ModelAttribute("receiverId") Long receiverId,
                           @ModelAttribute("amount") Double amount,
                           @ModelAttribute("description") String description) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountService.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        try {
            transactionService.makeTransaction(user.getId(), receiverId, new java.math.BigDecimal(amount), description);
            return "redirect:/transactions?success";
        } catch (IllegalArgumentException e) {
            String errorMessage = java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/transactions?error=" + errorMessage;
        }
    }

    @PostMapping("/connections/delete/{id}")
    public String deleteConnection(@PathVariable Long id) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        UserAccount user = userAccountService.findByEmail(auth.getName())
                .orElseThrow(() -> new IllegalArgumentException("Utilisateur non trouvé"));

        try {
            // Vérifier que la connexion existe
            if (connectionService.findById(id).isEmpty()) {
                return "redirect:/connections?error=connection_not_found";
            }

            // Vérifier que la connexion appartient bien à l'utilisateur actuel
            connectionService.findById(id).ifPresent(connection -> {
                if (!connection.getOwner().getId().equals(user.getId())) {
                    throw new IllegalArgumentException("Vous n'êtes pas autorisé à supprimer cette connexion");
                }
                connectionService.deleteConnection(id);
            });

            return "redirect:/connections?success_delete";
        } catch (IllegalArgumentException e) {
            String errorMessage = java.net.URLEncoder.encode(e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
            return "redirect:/connections?error=" + errorMessage;
        }
    }
}
