package interview.finance;

public class BalanceTransferService {

    void transfer(Account from, Account to, long amount) {
        Account first = from.getId().compareTo(to.getId()) < 0 ? from : to;
        Account second = from.getId().compareTo(to.getId()) < 0 ? to : from;

        if (from.getId().equals(to.getId())) {
            throw new IllegalStateException("Accounts should be different");
        } else if (amount <= 0) {
            throw new IllegalStateException("Amount should be positive");
        }

        first.getLock().lock();
        try {
            second.getLock().lock();
            try {
                if (from.getBalance() < amount) {
                    throw new IllegalStateException("Insufficient funds");
                }
                from.withdraw(amount);
                to.deposit(amount);
            } finally {
                second.getLock().unlock();
            }
        } finally {
            first.getLock().unlock();
        }
    }

}
