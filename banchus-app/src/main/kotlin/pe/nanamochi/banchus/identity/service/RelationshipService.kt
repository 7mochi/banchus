package pe.nanamochi.banchus.identity.service

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.toResultOr
import org.springframework.stereotype.Service
import pe.nanamochi.banchus.identity.entity.Relationship
import pe.nanamochi.banchus.identity.entity.User
import pe.nanamochi.banchus.identity.repository.RelationshipRepository
import pe.nanamochi.banchus.core.error.DomainMessage
import pe.nanamochi.banchus.core.error.RelationshipNotFound
import pe.nanamochi.banchus.core.util.runDatabaseCatching

@Service
class RelationshipService(private val relationshipRepository: RelationshipRepository) {
    fun fetchOne(followerId: Int, friendId: Int): Result<Relationship, DomainMessage> =
        relationshipRepository.findByFollowerIdAndFriendId(followerId, friendId).toResultOr {
            RelationshipNotFound
        }

    fun fetchFriends(user: User): Result<List<Relationship>, DomainMessage> =
        relationshipRepository.findAllByFollower(user).toResultOr { RelationshipNotFound }

    fun addFriend(followerId: Int, toAdd: Int): Result<Relationship, DomainMessage> =
        runDatabaseCatching {
            relationshipRepository.findByFollowerIdAndFriendId(followerId, toAdd)
                ?: relationshipRepository.save(
                    Relationship(follower = User(id = followerId), friend = User(id = toAdd))
                )
        }

    fun removeFriend(followerId: Int, toRemoveId: Int): Result<Unit, DomainMessage> =
        relationshipRepository.deleteByFollowerIdAndFriendId(followerId, toRemoveId).toResultOr {
            RelationshipNotFound
        }
}
