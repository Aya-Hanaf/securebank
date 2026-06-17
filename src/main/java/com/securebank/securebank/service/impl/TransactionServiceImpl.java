package com.securebank.securebank.service.impl;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.securebank.securebank.dto.request.DepositRequest;
import com.securebank.securebank.dto.request.TransferRequest;
import com.securebank.securebank.dto.request.WithdrawRequest;
import com.securebank.securebank.dto.response.TransactionResponse;
import com.securebank.securebank.entity.Account;
import com.securebank.securebank.entity.AccountStatus;
import com.securebank.securebank.entity.Transaction;
import com.securebank.securebank.entity.TransactionStatus;
import com.securebank.securebank.entity.TransactionType;
import com.securebank.securebank.exception.AppException;
import com.securebank.securebank.mapper.TransactionMapper;
import com.securebank.securebank.repository.AccountRepository;
import com.securebank.securebank.repository.TransactionRepository;
import com.securebank.securebank.service.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final TransactionMapper transactionMapper;

    // ── Write operations ──────────────────────────────────────────────────────

    @Override
    @Transactional
    public TransactionResponse deposit(DepositRequest request, UUID userId) {
        Account account = lockedOwnedAccount(request.accountId(), userId);
        requireActive(account);

        account.setBalance(account.getBalance().add(request.amount()));
        accountRepository.save(account);

        return transactionMapper.toResponse(transactionRepository.save(
                buildTransaction(TransactionType.CREDIT, request.amount(),
                        request.description(), account, null)));
    }

    @Override
    @Transactional
    public TransactionResponse withdraw(WithdrawRequest request, UUID userId) {
        Account account = lockedOwnedAccount(request.accountId(), userId);
        requireActive(account);
        requireSufficientFunds(account, request.amount());

        account.setBalance(account.getBalance().subtract(request.amount()));
        accountRepository.save(account);

        return transactionMapper.toResponse(transactionRepository.save(
                buildTransaction(TransactionType.DEBIT, request.amount(),
                        request.description(), account, null)));
    }

    @Override
    @Transactional
    public TransactionResponse transfer(TransferRequest request, UUID userId) {
        // Lock accounts in UUID order to prevent deadlocks under concurrent transfers
        Account source = lockedOwnedAccount(request.sourceAccountId(), userId);
        requireActive(source);
        requireSufficientFunds(source, request.amount());

        if (source.getAccountNumber().equals(request.targetAccountNumber())) {
            throw new AppException(HttpStatus.BAD_REQUEST,
                    "Source and target accounts must be different");
        }

        Account target = accountRepository
                .findByAccountNumberForUpdate(request.targetAccountNumber())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Target account not found"));
        requireActive(target);

        source.setBalance(source.getBalance().subtract(request.amount()));
        target.setBalance(target.getBalance().add(request.amount()));
        accountRepository.save(source);
        accountRepository.save(target);

        // Debit leg on source — targetAccountNumber shows where money went
        Transaction debit = buildTransaction(TransactionType.TRANSFER, request.amount(),
                request.description(), source, target.getAccountNumber());

        // Credit leg on target — targetAccountNumber shows where money came from
        Transaction credit = buildTransaction(TransactionType.TRANSFER, request.amount(),
                request.description(), target, source.getAccountNumber());

        transactionRepository.save(debit);
        transactionRepository.save(credit);

        log.info("Transfer {} → {} amount={} ref={}",
                source.getAccountNumber(), target.getAccountNumber(),
                request.amount(), debit.getReference());

        return transactionMapper.toResponse(debit);
    }

    // ── Read operations ───────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public TransactionResponse getTransactionById(UUID transactionId, UUID userId) {
        return transactionMapper.toResponse(
                transactionRepository.findByIdAndUserId(transactionId, userId)
                        .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                                "Transaction not found")));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponse> getAccountTransactions(UUID accountId, UUID userId) {
        return transactionMapper.toResponseList(
                transactionRepository.findByAccountIdAndUserId(accountId, userId));
    }

    // ── PDF receipt ───────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public byte[] generateReceipt(UUID transactionId, UUID userId) {
        Transaction tx = transactionRepository.findByIdAndUserId(transactionId, userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Transaction not found"));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            buildPdf(tx, baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("PDF generation failed for transaction {}", transactionId, e);
            throw new AppException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to generate receipt");
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private Account lockedOwnedAccount(UUID accountId, UUID userId) {
        return accountRepository.findByIdAndUserIdForUpdate(accountId, userId)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND,
                        "Account not found"));
    }

    private void requireActive(Account account) {
        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Account " + account.getAccountNumber() + " is not active");
        }
    }

    private void requireSufficientFunds(Account account, BigDecimal amount) {
        if (account.getBalance().compareTo(amount) < 0) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Insufficient funds in account " + account.getAccountNumber());
        }
    }

    private Transaction buildTransaction(TransactionType type, BigDecimal amount,
                                         String description, Account account,
                                         String targetAccountNumber) {
        return Transaction.builder()
                .reference(generateUniqueReference())
                .type(type)
                .amount(amount)
                .description(description)
                .status(TransactionStatus.COMPLETED)
                .account(account)
                .targetAccountNumber(targetAccountNumber)
                .build();
    }

    private String generateUniqueReference() {
        String ref;
        do {
            ref = "TXN" + System.currentTimeMillis()
                    + ThreadLocalRandom.current().nextInt(1000, 9999);
        } while (transactionRepository.existsByReference(ref));
        return ref;
    }

    // ── PDF builder ───────────────────────────────────────────────────────────

    private void buildPdf(Transaction tx, ByteArrayOutputStream baos) {
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);

        try (Document doc = new Document(pdf, PageSize.A4)) {
            doc.setMargins(50, 50, 50, 50);

            // ── Header ────────────────────────────────────────────────────
            doc.add(new Paragraph("SECUREBANK")
                    .setFontSize(22)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.DARK_GRAY));

            doc.add(new Paragraph("Transaction Receipt")
                    .setFontSize(14)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setFontColor(ColorConstants.GRAY)
                    .setMarginBottom(20));

            // ── Details table ─────────────────────────────────────────────
            Table table = new Table(UnitValue.createPercentArray(new float[]{35, 65}))
                    .useAllAvailableWidth()
                    .setMarginBottom(20);

            addRow(table, "Reference",   tx.getReference());
            addRow(table, "Type",        tx.getType().name());
            addRow(table, "Status",      tx.getStatus().name());
            addRow(table, "Amount",      tx.getAmount().toPlainString()
                                         + " " + tx.getAccount().getCurrency());
            addRow(table, "Account",     tx.getAccount().getAccountNumber());

            if (tx.getTargetAccountNumber() != null) {
                String label = tx.getType() == TransactionType.TRANSFER
                        && tx.getAccount().getBalance()
                             .compareTo(BigDecimal.ZERO) >= 0
                        ? "Counterpart" : "Counterpart";
                addRow(table, label, tx.getTargetAccountNumber());
            }

            addRow(table, "Description",
                    tx.getDescription() != null ? tx.getDescription() : "—");
            addRow(table, "Date",
                    tx.getCreatedAt().format(DATE_FMT));

            doc.add(table);

            // ── Footer ────────────────────────────────────────────────────
            doc.add(new Paragraph(
                    "This is a digitally generated receipt. No signature required.")
                    .setFontSize(9)
                    .setFontColor(ColorConstants.GRAY)
                    .setTextAlignment(TextAlignment.CENTER));
        }
    }

    private void addRow(Table table, String label, String value) {
        table.addCell(new Cell()
                .add(new Paragraph(label).setBold())
                .setBackgroundColor(ColorConstants.LIGHT_GRAY)
                .setPadding(6));
        table.addCell(new Cell()
                .add(new Paragraph(value))
                .setPadding(6));
    }
}
