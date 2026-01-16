import { useNavigate } from 'react-router-dom';
import { ArrowLeft } from 'lucide-react';
import { Button } from '../ui/button';

export function PrivacyView() {
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
          <h1 className="ml-2 text-lg font-semibold text-stone-800">개인정보처리방침</h1>
        </div>
      </header>

      <div className="p-6 prose prose-stone prose-sm max-w-none">
        <p className="text-stone-500 text-sm">최종 수정일: 2024년 1월 1일</p>

        <h2 className="text-lg font-bold mt-6">1. 개인정보의 수집 항목 및 수집 방법</h2>
        <p>회사는 서비스 제공을 위해 다음과 같은 개인정보를 수집합니다:</p>
        
        <h3 className="font-semibold mt-4">필수 수집 항목</h3>
        <ul className="list-disc pl-5 space-y-1">
          <li>이메일 주소</li>
          <li>이름 (또는 닉네임)</li>
          <li>비밀번호</li>
        </ul>

        <h3 className="font-semibold mt-4">선택 수집 항목</h3>
        <ul className="list-disc pl-5 space-y-1">
          <li>연락처</li>
          <li>프로필 사진</li>
          <li>자기소개</li>
        </ul>

        <h2 className="text-lg font-bold mt-6">2. 개인정보의 이용 목적</h2>
        <p>수집한 개인정보는 다음의 목적을 위해 이용됩니다:</p>
        <ul className="list-disc pl-5 space-y-1">
          <li>회원 가입 및 관리</li>
          <li>서비스 제공 및 운영</li>
          <li>공지사항 및 서비스 관련 안내</li>
          <li>서비스 개선 및 새로운 서비스 개발</li>
          <li>불법적 이용 방지 및 분쟁 조정</li>
        </ul>

        <h2 className="text-lg font-bold mt-6">3. 개인정보의 보유 및 이용 기간</h2>
        <p>
          회사는 개인정보 수집 및 이용 목적이 달성된 후에는 해당 정보를 지체 없이 파기합니다.
          단, 관련 법령에 따라 보존할 필요가 있는 경우에는 해당 기간 동안 개인정보를 보관합니다.
        </p>
        <ul className="list-disc pl-5 space-y-1">
          <li>계약 또는 청약철회 등에 관한 기록: 5년</li>
          <li>대금결제 및 재화 등의 공급에 관한 기록: 5년</li>
          <li>소비자의 불만 또는 분쟁처리에 관한 기록: 3년</li>
        </ul>

        <h2 className="text-lg font-bold mt-6">4. 개인정보의 제3자 제공</h2>
        <p>
          회사는 이용자의 개인정보를 원칙적으로 외부에 제공하지 않습니다. 
          다만, 다음의 경우에는 예외로 합니다:
        </p>
        <ul className="list-disc pl-5 space-y-1">
          <li>이용자가 사전에 동의한 경우</li>
          <li>법령의 규정에 의하거나, 수사 목적으로 법령에 정해진 절차와 방법에 따라 요청이 있는 경우</li>
        </ul>

        <h2 className="text-lg font-bold mt-6">5. 개인정보의 파기 절차 및 방법</h2>
        <p>
          회사는 개인정보 보유기간의 경과, 처리목적 달성 등 개인정보가 불필요하게 되었을 때에는 
          지체 없이 해당 개인정보를 파기합니다.
        </p>
        <ul className="list-disc pl-5 space-y-1">
          <li>전자적 파일: 복구 및 재생되지 않도록 안전하게 삭제</li>
          <li>종이 문서: 분쇄기로 분쇄하거나 소각</li>
        </ul>

        <h2 className="text-lg font-bold mt-6">6. 이용자의 권리와 행사 방법</h2>
        <p>이용자는 다음과 같은 권리를 행사할 수 있습니다:</p>
        <ul className="list-disc pl-5 space-y-1">
          <li>개인정보 열람 요구</li>
          <li>오류 등이 있을 경우 정정 요구</li>
          <li>삭제 요구</li>
          <li>처리정지 요구</li>
        </ul>

        <h2 className="text-lg font-bold mt-6">7. 개인정보 보호책임자</h2>
        <p>
          회사는 개인정보 처리에 관한 업무를 총괄해서 책임지고, 개인정보 처리와 관련한 
          이용자의 불만처리 및 피해구제를 위하여 아래와 같이 개인정보 보호책임자를 지정하고 있습니다.
        </p>
        <div className="bg-stone-50 p-4 rounded-lg mt-4">
          <p className="text-sm">
            <strong>개인정보 보호책임자</strong><br />
            이메일: privacy@example.com<br />
            연락처: 02-0000-0000
          </p>
        </div>

        <div className="mt-8 p-4 bg-orange-50 rounded-lg">
          <p className="text-sm text-orange-800">
            본 개인정보처리방침은 2024년 1월 1일부터 적용됩니다.<br />
            변경 사항이 있을 경우 서비스 내 공지사항을 통해 안내드리겠습니다.
          </p>
        </div>
      </div>
    </div>
  );
}



