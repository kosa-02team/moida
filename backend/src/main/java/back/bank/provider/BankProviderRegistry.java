package back.bank.provider;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BankProviderRegistry {

    private final Map<String, BankProvider> providers;

    public BankProviderRegistry(List<BankProvider> providerList) {
        this.providers = providerList.stream()
                .collect(Collectors.toMap(BankProvider::bankCode, Function.identity()));
    }

    public BankProvider get(String bankCode) {
        BankProvider provider = providers.get(bankCode);
        if (provider == null) {
            throw new IllegalArgumentException("Unsupported bankCode: " + bankCode);
        }
        return provider;
    }
}