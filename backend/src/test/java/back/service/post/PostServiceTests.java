package back.service.post;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

public class PostServiceTests {

    @Nested
    @DisplayName("모임 게시글 전체 조회")
    class ListPosts { /* 목록 */

        @Test
        @DisplayName("[GUEST] 공개 모임 게시글 조회 성공")
        //검색이 허용된 모임에서 게사글이 공개일 때
        void list_club_public_posts_public(){

        }

        @Test
        @DisplayName("[GUEST] 공개 모임 게시글 조회 성공 - 비공개 게시글")
        //검색이 허용된 모임에서 게사글이 비공개일 때, 비공개 게시글이라는 200 코드와 실제로 반환은 안됨
        void list_club_public_posts_private(){

        }

        @Test
        @DisplayName("[MEMBER] 비공개 모임 게시글 조회 성공")
        //모임 멤버가 게시글 확인
        void list_club_private(){

        }
    }


    @Nested
    class GetPostDetail { /* 상세 조회 */

        @Test
        @DisplayName("[GUEST] 공개 모임 게시글 상세 조회 성공")
        void get_post_public(){
            
        }

        @Test
        @DisplayName("[GUEST] 게시글 상세 조회 실패 - 없는 게시글")
        void get_no_post_guest(){

        }

        @Test
        @DisplayName("[GUEST] 공개 모임 게시글 상세 조회 실패 - 비공개 게시글")
        void get_post_private_guest(){

        }

        @Test
        @DisplayName("[MEMBER] 비공개 모임 게시글 상세 조회 성공")
        void get_post_private_member(){

        }

        @Nested
        @DisplayName("게시글 내 댓글 전체 조회")
        class getComments{

            @Test
            @DisplayName("[MEMBER] 공개모임 게시글 내 댓글 조회 성공")
            void get_comments(){

            }

            @Test
            @DisplayName("[GUEST] 공개모임 게시글 내 댓글 조회 실패 - 댓글 조회 권한 없음")
            void get_comments_guest(){

            }
        }

        @Nested
        @DisplayName("게시글 내 좋아요 수 조회")
        class getLikes{

            @Test
            @DisplayName("[MEMBER] 게시글 내 좋아수 수 조회 성공")
            void get_likes(){

            }
        }

    }

    @Nested
    class CreatePost { /* 게시글 생성 */

        @Test
        @DisplayName("[MEMBER] 모임 게시글 생성 성공")
        void create_post_member(){

        }

        @Test
        @DisplayName("[GUEST] 모임 게시글 생성 실패 - 게시글 생성 권한 없음")
        void create_post_guest(){

        }

        @Nested
        class CreateComment{ /*댓글 생성*/
            
            @Test
            @DisplayName("[MEMBER] 댓글 생성 성공")
            void create_comment_member(){
                
            }

            @Test
            @DisplayName("[GUEST] 댓글 생성 실패 - 댓글 생성 권한 없음")
            void create_comment_guest(){

            }
            
        }

        @Nested
        class CreateLike{

            @Test
            @DisplayName("[MEMBER] 좋아요 토글 성공")
            void create_like(){

            }

            @Test
            @DisplayName("[GUEST] 좋아요 생성 실패 - 좋아요 생성 권한 없음")
            void create_like_guest(){

            }

            @Test
            @DisplayName("[MEMBER] 좋아요 토글 성공 - 기존 좋아요 취소")
            //이미 좋아요를 누른 사람이 한번 더 누르면 취소가 되게
            void create_like_duplicate(){

            }
        }
    }

    @Nested
    class UpdatePost { /* 수정 */

        @Test
        @DisplayName("[MEMBER] 모임 게시글 수정 성공")
        void update_post_member(){

        }

        @Test
        @DisplayName("[ADMIN] 모임 게시글 수정 성공")
        //admin은 게시글 상태만 변경 가능
        void update_post_admin_ban(){

        }

        @Test
        @DisplayName("[ADMIN] 모임 게시글 수정 실패 - 게시글 수정 권한 없음")
        //admin은 게시글 상태만 변경 가능
        void update_post_admin(){

        }

        @Test
        @DisplayName("[MEMBER] 모임 게시글 수정 실패 - 게시글 수정 권한 없음, 작성자 아님")
        void update_post_member_not_writer(){

        }
        
        @Nested
        class UpdateComment{
            
            @Test
            @DisplayName("[MEMBER] 댓글 수정 성공")
            void update_comment(){
                
            }

            @Test
            @DisplayName("[MEMBER] 댓글 수정 실패 - 없는 댓글")
            void update_no_comment(){

            }

            @Test
            @DisplayName("[MEMBER] 댓글 수정 실패 - 댓글 수정 권한 없음, 작성자 아님")
            void update_comment_not_writer(){
                
            }

            @Test
            @DisplayName("[ADMIN] 댓글 수정 실패 - 댓글 텍스트 수정 권한 없음")
            //관리자는 댓글 금지만 변경 가능
            void update_comment_admin(){

            }
        }
    }

    @Nested
    class DeletePost { /* 삭제 */

        @Test
        @DisplayName("[MEMBER] 모임 게시글 삭제 성공")
        void delete_post_member(){

        }

        @Test
        @DisplayName("[MEMBER] 모임 게시글 삭제 실패 - 게시글 삭제 권한 없음, 작성자 아님")
        void delete_post_guest(){

        }
        
        @Nested
        class DeleteComment{
            
            @Test
            @DisplayName("[MEMBER] 모임 댓글 삭제 성공")
            void delete_comment_member(){
                
            }

            @Test
            @DisplayName("[MEMBER] 모임 댓글 삭제 실패 - 게시글 삭제 권한 없음, 작성자 아님")
            void delete_comment_member_not_writer(){

            }
        }
    }

}
