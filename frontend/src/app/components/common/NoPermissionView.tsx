import {useNavigate} from 'react-router-dom';
import {Lock, ArrowLeft, Home, UserPlus} from 'lucide-react';
import {Button} from '../ui/button';

interface NoPermissionViewProps {
    type?: 'posts' | 'dues' | 'members' | 'admin' | 'general',
    groupName?: string,
    showJoinButton?: boolean,
    onJoinClick?: () => void,
    message?: string
}

const messages = {
    posts: {
        title: '게시글을 볼 수 없습니다',
        description: '이 모임의 게시글은 회원만 볼 수 있습니다.',
    },
    dues: {
        title: '회비 현황을 볼 수 없습니다',
        description: '회비 현황은 모임 회원만 확인할 수 있습니다.',
    },
    members: {
        title: '멤버 목록을 볼 수 없습니다',
        description: '이 모임의 멤버 목록은 회원만 볼 수 있습니다.',
    },
    admin: {
        title: '접근 권한이 없습니다',
        description: '이 페이지는 관리자만 접근할 수 있습니다.',
    },
    general: {
        title: '접근 권한이 없습니다',
        description: '이 콘텐츠를 보려면 모임에 가입해야 합니다.',
    },
};

export function NoPermissionView({
                                     type = 'general',
                                     groupName,
                                     showJoinButton = true,
                                     onJoinClick
                                 }: NoPermissionViewProps) {
    const navigate = useNavigate();
    const message = messages[type];

    return (
        <div className="min-h-[60vh] flex flex-col items-center justify-center p-6 text-center">
            <div className="w-20 h-20 bg-orange-100 rounded-full flex items-center justify-center mb-6">
                <Lock className="w-10 h-10 text-orange-500"/>
            </div>

            <h2 className="text-xl font-bold text-stone-900 mb-2">
                {message.title}
            </h2>
            <p className="text-stone-500 mb-6 max-w-xs">
                {message.description}
            </p>

            {groupName && (
                <p className="text-sm text-stone-600 mb-6">
                    모임: <span className="font-medium">{groupName}</span>
                </p>
            )}

            <div className="flex flex-col sm:flex-row gap-3 w-full max-w-xs">
                {showJoinButton && (
                    <Button
                        onClick={onJoinClick || (() => navigate(-1))}
                        className="flex-1 h-12 bg-orange-500 hover:bg-orange-600 text-white rounded-xl"
                    >
                        <UserPlus className="w-5 h-5 mr-2"/>
                        가입 신청하기
                    </Button>
                )}
                <Button
                    variant="outline"
                    onClick={() => navigate(-1)}
                    className="flex-1 h-12 rounded-xl"
                >
                    <ArrowLeft className="w-5 h-5 mr-2"/>
                    이전으로
                </Button>
            </div>

            <Button
                variant="ghost"
                onClick={() => navigate('/')}
                className="mt-4 text-stone-500"
            >
                <Home className="w-4 h-4 mr-2"/>
                홈으로 가기
            </Button>
        </div>
    );
}



