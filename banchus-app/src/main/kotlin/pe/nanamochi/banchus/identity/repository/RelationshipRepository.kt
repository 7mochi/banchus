package pe.nanamochi.banchus.identity.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import pe.nanamochi.banchus.identity.entity.Relationship
import pe.nanamochi.banchus.identity.entity.User

@Repository
interface RelationshipRepository : JpaRepository<Relationship, Int> {
    fun findByFollowerIdAndFriendId(followerId: Int, friendId: Int): Relationship?

    fun findAllByFollower(follower: User): List<Relationship>

    fun deleteByFollowerIdAndFriendId(followerId: Int, friendId: Int)

    fun existsByFollowerAndFriend(follower: User, friend: User): Boolean
}
