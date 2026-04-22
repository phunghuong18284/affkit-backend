package vn.affkit.telegram;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import vn.affkit.accesstrade.AccessTradeService;
import vn.affkit.accesstrade.AccessTradeService.ConvertResult;

@Slf4j
@Component
public class TelegramBotService extends TelegramLongPollingBot {

    private final TelegramConfig telegramConfig;
    private final AccessTradeService accessTradeService;

    public TelegramBotService(TelegramConfig telegramConfig,
                              AccessTradeService accessTradeService) {
        super(telegramConfig.getBotToken());
        this.telegramConfig = telegramConfig;
        this.accessTradeService = accessTradeService;
    }

    @Override
    public String getBotUsername() {
        return telegramConfig.getBotUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) return;

        String text   = update.getMessage().getText().trim();
        String chatId = update.getMessage().getChatId().toString();
        String name   = update.getMessage().getFrom().getFirstName();

        if (text.equals("/start")) {
            reply(chatId, "Chao " + name + "! Gui link Lazada / TikTok cho minh de convert sang affiliate link nhe!");
            return;
        }

        if (text.startsWith("http://") || text.startsWith("https://")) {
            ConvertResult result = accessTradeService.convertLink(text);
            if (result.isSuccess()) {
                reply(chatId, "Affiliate link cua ban:\n" + result.affiliateUrl());
            } else if (result.status() == ConvertResult.Status.UNSUPPORTED) {
                reply(chatId, "San nay chua ho tro. Hien tai chi ho tro Lazada va TikTok.");
            } else {
                reply(chatId, "Khong the convert link nay. Vui long thu lai sau.");
            }
            return;
        }

        reply(chatId, "Vui long gui link san pham (bat dau bang https://)");
    }

    private void reply(String chatId, String text) {
        SendMessage message = SendMessage.builder()
                .chatId(chatId)
                .text(text)
                .build();
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Telegram send error: {}", e.getMessage());
        }
    }
}