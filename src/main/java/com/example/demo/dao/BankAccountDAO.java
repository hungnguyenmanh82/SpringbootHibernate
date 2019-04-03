package com.example.demo.dao;


import java.util.List;
 
import org.hibernate.Session;
import org.hibernate.SessionFactory;
import org.hibernate.query.Query;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.entity.BankAccount;
import com.example.demo.exception.BankTransactionException;
import com.example.demo.model.BankAccountInfo;
 
@Repository  //singleton
@Transactional
public class BankAccountDAO {
 
    /**
     * Hibernate là độc lập với Springboot => dùng như 1 thư viện Java thông thường
     * <xem vd: Hibernate> sẽ rõ
     * ở trường hợp này ta đang change config của JPA => Hibernate interface ở SpringBoot
     */
	@Autowired    // Spring's default scope is singleton
    private SessionFactory sessionFactory; //dùng Hibernate thay vì JPA ở chỗ này
 
    public BankAccountDAO() {
    }
 
    public BankAccount findById(Long id) {
        Session session = this.sessionFactory.getCurrentSession();
        return session.get(BankAccount.class, id);
    }
 
    public List<BankAccountInfo> listBankAccountInfo() {
    	/**
    	 * chỗ này dùng Hibernate ko phải JPA
    	 * Hibernate HQL language
    	 */
        String sql = "Select new " + BankAccountInfo.class.getName() //
                + "(e.id,e.fullName,e.balance) " //
                + " from " + BankAccount.class.getName() + " e ";
        
        /**
         * Lệnh này hoàn toàn ko dùng Transaction vẫn ok.
         */
        Session session = this.sessionFactory.getCurrentSession();
        Query<BankAccountInfo> query = session.createQuery(sql, BankAccountInfo.class);
        return query.getResultList();
    }
 
    /**
      Khài niệm transaction là khái niệm của SQL => vì thế đúng với cả JPA và Hibernate.
      Transaction là chạy nhiều lệnh ở SQL server, nếu gặp lỗi thì server hỗ trợ rollback lại toàn bộ, ở client sẽ xuất ra exception khi gặp lỗi.
      @transactional là của Springboot (ko phải của JPA), trc 1 function DAO để chỉ 1 transaction truy cập vào SQL server.
      Springboot hỗ trợ auto scan dùng java reflection => sau đó chèn thêm phần code transaction vào trong DAO (để giảm code).
      Vì thế @transactional hỗ trợ cả JPA, và Hibernate.
      Nếu ko dùng @transaction của Springboot, thì với JPA phải dùng như sau:
      
        //để dùng transaction của JPA explicit 
        UserTransaction utx = entityManager.getTransaction(); 

		try { 
		    utx.begin(); 
		    //nhiều lệnh sql: update, insert, delete
		    businessLogic();//xem code phía dưới viết lại gọn hơn
		    
		    utx.commit(); 
		} catch(Exception ex) { 
		    utx.rollback(); //cách này dễ debug khi gặp lỗi hơn cách dưới
		    throw ex; 
		} 

     */
    /**
     *  @Transactional là annotation của Springboot ko phải của JPA để chỉ việc bắt đầu và kết thúc Transaction gắn với việc
     *  bắt đầu vào kết thúc 1 java function.
     *  Hibernate vẫn tận dụng các annotation của JPA
     */
    // MANDATORY: Transaction must be created before.
    @Transactional(propagation = Propagation.MANDATORY)
    public void addAmount(Long id, double amount) throws BankTransactionException {
        BankAccount account = this.findById(id);
        if (account == null) {
            throw new BankTransactionException("Account not found " + id);
        }
        double newBalance = account.getBalance() + amount;
        if (account.getBalance() + amount < 0) {
            throw new BankTransactionException(
                    "The money in the account '" + id + "' is not enough (" + account.getBalance() + ")");
        }
        account.setBalance(newBalance);
    }
 
    // Do not catch BankTransactionException in this method.
    @Transactional(propagation = Propagation.REQUIRES_NEW, rollbackFor = BankTransactionException.class)
    public void sendMoney(Long fromAccountId, Long toAccountId, double amount) throws BankTransactionException {
 
        addAmount(toAccountId, amount);  //function này chứa @transactional, vì thế parent function sẽ ko tạo source transactional nữa
        addAmount(fromAccountId, -amount);
    }
 
}
