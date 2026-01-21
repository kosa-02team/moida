import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { Button } from '../ui/button';

export function TermsView() {
  const navigate = useNavigate();

  return (
    <div className="min-h-screen bg-white">
      {/* Header */}
      <header className="sticky top-0 z-30 bg-white border-b border-stone-100">
        <div className="flex items-center px-4 py-3">
          <Button
            variant="ghost"
            size="icon"
            onClick={() => navigate(-1)}
            className="-ml-2"
          >
            <ArrowLeft className="w-6 h-6 text-stone-800" />
          </Button>
          <h1 className="ml-2 text-lg font-semibold text-stone-800">이용약관</h1>
        </div>
      </header>

      <div className="p-6 prose prose-stone prose-sm max-w-none">
        <p className="text-stone-500 text-sm">시행일: 2024년 1월 1일</p>

        <h2 className="text-lg font-bold mt-6">제1조 (목적)</h2>
        <p>
          본 약관은 모임 관리 서비스(이하 "서비스")를 이용함에 있어 회사와 이용자의 권리,
          의무 및 책임사항을 규정함을 목적으로 합니다.
        </p>

        <h2 className="text-lg font-bold mt-6">제2조 (정의)</h2>
        <p>① "서비스"란 회사가 제공하는 모임 관리 관련 모든 서비스를 의미합니다.</p>
        <p>② "이용자"란 서비스에 접속하여 본 약관에 따라 서비스를 이용하는 회원 및 비회원을 말합니다.</p>
        <p>③ "회원"이란 서비스에 회원등록을 한 자로서, 계속적으로 서비스를 이용할 수 있는 자를 말합니다.</p>

        <h2 className="text-lg font-bold mt-6">제3조 (약관의 효력 및 변경)</h2>
        <p>
          ① 본 약관은 서비스를 이용하고자 하는 모든 이용자에게 적용됩니다.<br />
          ② 회사는 필요한 경우 관련 법령을 위배하지 않는 범위 내에서 본 약관을 변경할 수 있습니다.<br />
          ③ 변경된 약관은 서비스 내 공지사항을 통해 공지합니다.
        </p>

        <h2 className="text-lg font-bold mt-6">제4조 (회원가입)</h2>
        <p>
          ① 이용자는 회사가 정한 가입 양식에 따라 회원정보를 기입한 후 본 약관에 동의한다는 
          의사표시를 함으로써 회원가입을 신청합니다.<br />
          ② 회사는 제1항과 같이 회원으로 가입할 것을 신청한 이용자 중 다음 각 호에 해당하지 않는 한 
          회원으로 등록합니다.
        </p>

        <h2 className="text-lg font-bold mt-6">제5조 (서비스의 제공)</h2>
        <p>회사는 다음과 같은 서비스를 제공합니다:</p>
        <ul className="list-disc pl-5 space-y-1">
          <li>모임 생성 및 관리</li>
          <li>일정 관리 및 투표</li>
          <li>회비 관리</li>
          <li>멤버 관리</li>
          <li>스토리 공유</li>
          <li>기타 회사가 정하는 서비스</li>
        </ul>

        <h2 className="text-lg font-bold mt-6">제6조 (개인정보보호)</h2>
        <p>
          회사는 이용자의 개인정보를 보호하기 위해 개인정보처리방침을 수립하고 이를 준수합니다.
          자세한 내용은 개인정보처리방침을 참고해 주시기 바랍니다.
        </p>

        <h2 className="text-lg font-bold mt-6">제7조 (이용자의 의무)</h2>
        <p>이용자는 다음 각 호의 행위를 하여서는 안 됩니다:</p>
        <ul className="list-disc pl-5 space-y-1">
          <li>타인의 정보 도용</li>
          <li>서비스의 운영을 방해하는 행위</li>
          <li>타인의 명예를 훼손하거나 불이익을 주는 행위</li>
          <li>외설 또는 폭력적인 내용의 정보 게시</li>
          <li>기타 관련 법령에 위배되는 행위</li>
        </ul>

        <h2 className="text-lg font-bold mt-6">제8조 (면책조항)</h2>
        <p>
          ① 회사는 천재지변 또는 이에 준하는 불가항력으로 인하여 서비스를 제공할 수 없는 경우에는 
          서비스 제공에 관한 책임이 면제됩니다.<br />
          ② 회사는 이용자의 귀책사유로 인한 서비스 이용의 장애에 대하여는 책임을 지지 않습니다.
        </p>

        <div className="mt-8 p-4 bg-stone-50 rounded-lg">
          <p className="text-sm text-stone-600">
            본 약관은 2024년 1월 1일부터 시행됩니다.<br />
            문의사항이 있으시면 support@example.com으로 연락해 주세요.
          </p>
        </div>
      </div>
    </div>
  );
}



