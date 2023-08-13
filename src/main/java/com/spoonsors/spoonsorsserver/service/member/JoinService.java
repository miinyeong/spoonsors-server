package com.spoonsors.spoonsorsserver.service.member;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.spoonsors.spoonsorsserver.controller.authorize.SmsController;
import com.spoonsors.spoonsorsserver.entity.authorize.MessageDto;
import com.spoonsors.spoonsorsserver.loginInfra.JwtTokenProvider;
import com.spoonsors.spoonsorsserver.repository.BMemberRepository;
import com.spoonsors.spoonsorsserver.repository.ISMemberRepository;
import com.spoonsors.spoonsorsserver.repository.IbMemberRepository;
import com.spoonsors.spoonsorsserver.repository.SMemberRepository;
import com.spoonsors.spoonsorsserver.service.authorize.SmsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Transactional
@Service
public class JoinService {
    private final IbMemberRepository ibMemberRepository;
    private final ISMemberRepository isMemberRepository;
    private final BMemberRepository bMemberRepository;
    private final SMemberRepository sMemberRepository;
    private final JwtTokenProvider jwtTokenProvider;

    private final SmsController smsController;

    public String checkId(String id) {
        if (ibMemberRepository.findById(id).isPresent()) {
            return "이미 존재하는 아이디입니다.";
        }
        if (isMemberRepository.findById(id).isPresent()) {
            return "이미 존재하는 아이디입니다.";
        }

        return "사용 가능한 아이디입니다.";
    }

    public String checkNickname(String nickname) {

        if (bMemberRepository.findByNickname(nickname).isPresent()) {
            return "이미 존재하는 닉네임입니다.";
        }
        if (sMemberRepository.findByNickname(nickname).isPresent()) {
            return "이미 존재하는 닉네임입니다.";
        }
        return "사용 가능한 닉네임입니다.";
    }

    public String getAccessToken(String authorize_code) throws Exception {
        String access_Token = "";
        String reqURL = "https://kauth.kakao.com/oauth/token";

        try {
            URL url = new URL(reqURL);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            // POST 요청을 위해 기본값이 false인 setDoOutput을 true로

            conn.setRequestMethod("POST");
            conn.setDoOutput(true);
            // POST 요청에 필요로 요구하는 파라미터 스트림을 통해 전송

            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(conn.getOutputStream()));

            String sb = "grant_type=authorization_code" +
                    "&client_id=03bbdf71352156f08fd91cdbd4b861e1" + // REST_API키
                    "&redirect_uri=http://3.86.110.15:8080/join/kakao" + // REDIRECT_URI
                    "&code=" + authorize_code;
            bw.write(sb);
            bw.flush();


            // 요청을 통해 얻은 JSON타입의 Response 메세지 읽어오기
            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String line = "";
            String result = "";

            while ((line = br.readLine()) != null) {
                result += line;
            }

            // jackson objectmapper 객체 생성
            ObjectMapper objectMapper = new ObjectMapper();
            // JSON String -> Map
            Map<String, Object> jsonMap = objectMapper.readValue(result, new TypeReference<>() {
            });

            access_Token = jsonMap.get("access_token").toString();


            br.close();
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return access_Token;
    }

    //카카오톡 유저 정보 가져오기
    public HashMap<String, String> getUserInfo(String access_Token) throws Throwable {
        HashMap<String, String> userInfo = new HashMap<>();
        String reqURL = "https://kapi.kakao.com/v2/user/me";

        try {
            URL url = new URL(reqURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");

            // 요청에 필요한 Header에 포함될 내용
            conn.setRequestProperty("Authorization", "Bearer " + access_Token);

            BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream()));

            String line = "";
            String result = "";

            while ((line = br.readLine()) != null) {
                result += line;
            }

            try {

                ObjectMapper objectMapper = new ObjectMapper();

                Map<String, Object> jsonMap = objectMapper.readValue(result, new TypeReference<>() {
                });

                System.out.println(jsonMap.get("properties"));

                Map<String, Object> properties = (Map<String, Object>) jsonMap.get("properties");
                Map<String, Object> kakao_account = (Map<String, Object>) jsonMap.get("kakao_account");


                String nickname = properties.get("nickname").toString();
                String email = kakao_account.get("email").toString();
                String profile_image = properties.get("profile_image").toString();

                userInfo.put("nickname", nickname);
                userInfo.put("email", email);
                userInfo.put("profile_image", profile_image);

            } catch (Exception e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
        return userInfo;
    }

    public String loginOrJoin(HashMap<String, String> info) {
        String id = info.get("email");

        if (ibMemberRepository.findById(id).isPresent()) {
            List<String> roles = new ArrayList<>();
            roles.add("BMEMBER");

            return jwtTokenProvider.createToken(id, roles);
        } else if (isMemberRepository.findById(id).isPresent()) {
            List<String> roles = new ArrayList<>();
            roles.add("SMEMBER");

            return jwtTokenProvider.createToken(id, roles);
        } else {
            return "회원가입이 필요합니다." + info;
        }
    }

    public String findId(HttpServletRequest request, String name, String phoneNum) throws UnsupportedEncodingException, URISyntaxException, NoSuchAlgorithmException, InvalidKeyException, JsonProcessingException {
        {
            if (bMemberRepository.findId(name, phoneNum).isPresent()) {
                MessageDto messageDto = new MessageDto(phoneNum);
                smsController.sendSms(request, messageDto);
                return "사용자 확인 완료";
            }
            if (sMemberRepository.findId(name, phoneNum).isPresent()) {
                MessageDto messageDto = new MessageDto(phoneNum);
                smsController.sendSms(request, messageDto);
                return "사용자 확인 완료";
            }
            return "이름과 번호가 일치하는 아이디가 없습니다.";
        }
    }
    public String verifyFindId(String name, String phoneNum) throws UnsupportedEncodingException, URISyntaxException, NoSuchAlgorithmException, InvalidKeyException, JsonProcessingException {
        {
            if (bMemberRepository.findId(name, phoneNum).isPresent()) {
                return bMemberRepository.findId(name, phoneNum).get().getBMember_id();
            }else
                return sMemberRepository.findId(name, phoneNum).get().getSMember_id();
            }
        }
}

