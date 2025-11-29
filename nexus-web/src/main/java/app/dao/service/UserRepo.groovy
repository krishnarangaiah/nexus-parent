package app.dao.service

import app.dao.model.user.AppUser
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository

@Repository
interface UserRepo extends JpaRepository<AppUser, Long> {

    @Query("SELECT u FROM AppUser u WHERE u.userName = ?1")
    List<AppUser> findByUserName(String userName);

}